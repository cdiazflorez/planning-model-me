package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.MAX_CURRENT_PHOTO_AGE_IN_MINUTES;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.NO_AREA;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.mergeMaps;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.selectPhotos;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.BacklogWorkflow;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails.BacklogProvider;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionHeadcount;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountAtArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountBySubArea;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class PickingDetailsBacklogService implements BacklogProvider {

  private static final String NA_AREA = "NA";

  private static final int MAX_AREA_NAME_SIZE = 2;

  private static final Duration DEFAULT_HOURS_LOOKBACK = Duration.ofHours(2);

  private final BacklogPhotoApiGateway backlogPhotoApiGateway;

  private final ProjectBacklog projectBacklog;


  private final GetProjectionHeadcount getProjectionHeadcount;

  @Override
  public boolean canProvide(final ProcessName process) {
    return process == PICKING;
  }

  @Override
  public Map<Instant, List<NumberOfUnitsInAnArea>> getMonitorBacklog(final BacklogProviderInput input) {
    final var currentBacklog = getCurrentBacklog(input);
    final var mappedBacklog = mapPhotosToNumberOfUnitsInAnAreaByTakenOn(currentBacklog.getOrDefault(PICKING, emptyList()));

    final var pastBacklog = selectPhotos(mappedBacklog, input.getDateFrom(), input.getRequestDate());
    final var projectedBacklog = getProjectedBacklog(input, currentBacklog, input.getThroughput(), input.getRequestDate());

    return mergeMaps(pastBacklog, projectedBacklog);
  }

  private Map<ProcessName, List<Photo>> getCurrentBacklog(final BacklogProviderInput input) {
    final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());
    return backlogPhotoApiGateway.getBacklogDetails(
        new BacklogRequest(
            input.getWarehouseId(),
            Set.of(input.getWorkflow()),
            Set.of(WAVING, PICKING),
            input.getRequestDate().truncatedTo(ChronoUnit.HOURS).minus(DEFAULT_HOURS_LOOKBACK),
            input.getRequestDate().truncatedTo(ChronoUnit.SECONDS),
            null,
            null,
            input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS),
            input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS),
            Set.of(STEP, AREA, DATE_OUT)
        )
    );
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> mapPhotosToNumberOfUnitsInAnAreaByTakenOn(final List<Photo> photos) {
    return photos.stream()
        .collect(
            Collectors.toMap(
                Photo::getTakenOn,
                this::mapPhotoGroupsToNumberOfUnitsInAnArea
            )
        );
  }

  private List<NumberOfUnitsInAnArea> mapPhotoGroupsToNumberOfUnitsInAnArea(final Photo photo) {
    final Map<String, Integer> unitsByArea = photo.getGroups()
        .stream()
        .collect(
            Collectors.toMap(
                group -> group.getGroupValue(AREA).orElse(NO_AREA),
                Photo.Group::getTotal,
                Integer::sum
            )
        );

    return unitsByArea.entrySet()
        .stream()
        .map(areaUnits -> new NumberOfUnitsInAnArea(areaUnits.getKey(), areaUnits.getValue()))
        .sorted(Comparator.comparing(NumberOfUnitsInAnArea::getArea))
        .collect(Collectors.toList());
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(final BacklogProviderInput input,
                                                                        final Map<ProcessName, List<Photo>> backlog,
                                                                        final List<MagnitudePhoto> throughput,
                                                                        final Instant viewDate) {

    final var currentBacklog = getCurrentBacklogAsQuantityAtSla(backlog, viewDate);

    try {
      return getProjectedBacklogByArea(input, currentBacklog, throughput);
    } catch (RuntimeException e) {
      log.error("could not retrieve backlog projections", e);
    }

    return emptyMap();
  }

  private List<BacklogQuantityAtSla> getCurrentBacklogAsQuantityAtSla(final Map<ProcessName, List<Photo>> backlog, final Instant viewDate) {
    final var currentPhotoDate = backlog.values()
        .stream()
        .flatMap(List::stream)
        .map(Photo::getTakenOn)
        .max(Comparator.naturalOrder())
        .filter(date -> ChronoUnit.MINUTES.between(date, viewDate) <= MAX_CURRENT_PHOTO_AGE_IN_MINUTES)
        .orElse(viewDate);

    return backlog.entrySet()
        .stream()
        .flatMap(entry -> entry.getValue()
            .stream()
            .filter(photo -> currentPhotoDate.equals(photo.getTakenOn()))
            .flatMap(photo -> mapPhotoGroupsToBacklogQuantityAtSla(
                photo.getGroups(),
                ProcessName.valueOf(entry.getKey().getName().toUpperCase(Locale.ENGLISH)))
            ))
        .collect(Collectors.toList());
  }

  private Stream<BacklogQuantityAtSla> mapPhotoGroupsToBacklogQuantityAtSla(final List<Photo.Group> groups, final ProcessName process) {
    return groups.stream()
        .collect(
            Collectors.collectingAndThen(
                Collectors.toMap(
                    group -> group.getGroupValue(DATE_OUT).map(Instant::parse).orElseThrow(),
                    Photo.Group::getTotal,
                    Integer::sum
                ),
                groupedGroups -> groupedGroups.entrySet()
                    .stream()
                    .map(e -> new BacklogQuantityAtSla(process, e.getKey(), e.getValue()))
            )
        );
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogByArea(final BacklogProviderInput input,
                                                                              final List<BacklogQuantityAtSla> backlog,
                                                                              final List<MagnitudePhoto> throughput) {

    final List<ProjectedBacklogForAnAreaAndOperatingHour> projections = projectBacklog.projectBacklogInAreas(
        input.getRequestDate(),
        input.getDateTo(),
        input.getWarehouseId(),
        input.getWorkflow(),
        List.of(WAVING, PICKING),
        backlog,
        throughput
    );

    final Map<Instant, List<NumberOfUnitsInAnArea>> pickingCarriedOverBacklog =
        groupPickingBacklogByStatus(projections, BacklogProcessStatus.CARRY_OVER, input.getRequestDate());

    final Map<Instant, List<NumberOfUnitsInAnArea>> pickingProcessedBacklog =
        groupPickingBacklogByStatus(projections, BacklogProcessStatus.PROCESSED, input.getRequestDate());

    final Map<Instant, List<HeadcountAtArea>> projectionHeadcount =
        getProjectionHeadcount.getProjectionHeadcount(input.getWarehouseId(), pickingProcessedBacklog);

    return assignHeadcount(pickingCarriedOverBacklog, projectionHeadcount);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> groupPickingBacklogByStatus(
      final List<ProjectedBacklogForAnAreaAndOperatingHour> projections,
      final BacklogProcessStatus backlogProcessStatus,
      final Instant requestDate) {

    final Map<Instant, List<ProjectedBacklogForAnAreaAndOperatingHour>> groupedProjections = projections.stream()
        .filter(projection -> projection.getStatus() == backlogProcessStatus
            && projection.getProcess() == PICKING
            && projection.getOperatingHour().isAfter(requestDate)
        )
        .collect(
            Collectors.groupingBy(
                ProjectedBacklogForAnAreaAndOperatingHour::getOperatingHour,
                Collectors.toList()
            )
        );

    return groupedProjections.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> toUnitsInArea(entry.getValue())
        ));
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> assignHeadcount(final Map<Instant, List<NumberOfUnitsInAnArea>> backlogs,
                                                                    final Map<Instant, List<HeadcountAtArea>> suggestedHeadCount) {

    return backlogs.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> {

          List<HeadcountAtArea> headcountAtAreaList = !suggestedHeadCount.isEmpty()
              ? suggestedHeadCount.get(entry.getKey())
              : emptyList();
          return entry.getValue()
              .stream()
              .map(value -> {
                Optional<HeadcountAtArea> headcountAtArea = headcountAtAreaList.stream()
                    .filter(headCount -> headCount.getArea().equals(value.getArea()))
                    .findAny();

                return new NumberOfUnitsInAnArea(
                    value.getArea(),
                    assignSubareas(value.getSubareas(), headcountAtArea.map(HeadcountAtArea::getSubAreas).orElse(emptyList())),
                    headcountAtArea.map(HeadcountAtArea::getReps).orElse(null),
                    headcountAtArea.map(HeadcountAtArea::getRespPercentage).orElse(null));
              })
              .collect(Collectors.toList());
        }));

  }

  private List<NumberOfUnitsInASubarea> assignSubareas(final List<NumberOfUnitsInASubarea> subareas,
                                                       final List<HeadcountBySubArea> subAreasHeadcount) {
    return subareas.stream()
        .map(value -> {
          Optional<HeadcountBySubArea> headcountBySubArea = subAreasHeadcount
              .stream()
              .filter(subArea -> subArea.getSubArea().equals(value.getName()))
              .findAny();

          return new NumberOfUnitsInASubarea(
              value.getName(),
              value.getUnits(),
              headcountBySubArea.map(HeadcountBySubArea::getReps).orElse(null),
              headcountBySubArea.map(HeadcountBySubArea::getRespPercentage).orElse(null));
        })
        .collect(Collectors.toList());
  }

  private List<NumberOfUnitsInAnArea> toUnitsInArea(final List<ProjectedBacklogForAnAreaAndOperatingHour> projections) {
    final long undefinedAreaQuantity = projections.stream()
        .filter(projection -> (NA_AREA).equals(projection.getArea()))
        .mapToLong(ProjectedBacklogForAnAreaAndOperatingHour::getQuantity)
        .sum();

    final long totalSubAreas = projections.stream()
        .map(ProjectedBacklogForAnAreaAndOperatingHour::getArea)
        .filter(area -> !(NA_AREA).equals(area))
        .distinct()
        .count();

    final long totalUnitsInAllAreas = projections.stream()
        .mapToLong(ProjectedBacklogForAnAreaAndOperatingHour::getQuantity)
        .sum();

    final long unitsInAllValidAreas = totalUnitsInAllAreas - undefinedAreaQuantity;

    final Map<String, List<NumberOfUnitsInASubarea>> subareas = projections.stream()
        .collect(
            Collectors.groupingBy(
                projection -> projection.getArea().substring(0, MAX_AREA_NAME_SIZE),
                Collectors.mapping(
                    projection -> getNumberOfUnitsInASubarea(projection, unitsInAllValidAreas, undefinedAreaQuantity, totalSubAreas),
                    Collectors.toList()
                )
            )
        );

    return subareas.entrySet()
        .stream()
        .map(entry -> new NumberOfUnitsInAnArea(entry.getKey(), entry.getValue()))
        .filter(areaUnits -> !NA_AREA.equals(areaUnits.getArea()))
        .collect(Collectors.toList());
  }

  private NumberOfUnitsInASubarea getNumberOfUnitsInASubarea(final ProjectedBacklogForAnAreaAndOperatingHour projection,
                                                             final long unitsInAllValidAreas,
                                                             final long undefinedAreaQuantity,
                                                             final long totalSubAreas) {

    if (unitsInAllValidAreas == 0) {
      return new NumberOfUnitsInASubarea(projection.getArea(), (int) (undefinedAreaQuantity / totalSubAreas));
    }

    final Long thisAreaBacklog = projection.getQuantity();
    final float undefinedAreaProportionalBacklog = (thisAreaBacklog / (float) unitsInAllValidAreas) * undefinedAreaQuantity;
    final long thisAreaAssignedBacklog = thisAreaBacklog + (long) undefinedAreaProportionalBacklog;

    return new NumberOfUnitsInASubarea(projection.getArea(), (int) thisAreaAssignedBacklog);
  }
}
