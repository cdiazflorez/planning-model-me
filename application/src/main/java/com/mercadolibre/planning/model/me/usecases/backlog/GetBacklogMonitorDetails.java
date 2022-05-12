package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.emptyMeasure;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromMinutes;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
import static java.lang.Math.max;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail.SubAreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.Headcount;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionHeadcount;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadCountByArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountBySubArea;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitorDetails extends GetConsolidatedBacklog {
  private static final String NO_AREA = "N/A";

  private static final String AREA_KEY = "area";

  private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
      FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING),
      FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
  );

  private static final int HOUR_TPH_FUTURE = 24;

  private final BacklogApiAdapter backlogApiAdapter;

  private final PlanningModelGateway planningModelGateway;

  private final GetProcessThroughput getProcessThroughput;

  private final GetHistoricalBacklog getHistoricalBacklog;

  private final GetBacklogLimits getBacklogLimits;

  private final ProjectBacklog projectBacklog;

  private final GetProjectionHeadcount getProjectionHeadcount;

  public GetBacklogMonitorDetailsResponse execute(final GetBacklogMonitorDetailsInput input) {
    final List<VariablesPhoto> backlog = getData(input);

    final Instant currentDatetime = getDateOfLatestNonProjectionBacklogPhoto(backlog, input.getRequestDate());

    final List<AreaName> areas = input.getProcess().hasAreas() ? areasPresentInBacklog(backlog) : emptyList();

    return new GetBacklogMonitorDetailsResponse(
        currentDatetime,
        getBacklogDetails(backlog, areas, currentDatetime),
        getResumedBacklog(input.getProcess(), currentDatetime, backlog)
    );
  }

  private List<AreaName> areasPresentInBacklog(final List<VariablesPhoto> backlog) {
    final Map<String, Set<String>> subareasByArea = backlog.stream()
        .map(VariablesPhoto::getAreas)
        .flatMap(entry -> entry.values().stream())
        .collect(
            Collectors.groupingBy(
                NumberOfUnitsInAnArea::getArea,
                Collectors.flatMapping(
                    allAreas -> allAreas.getSubareas()
                        .stream()
                        .map(NumberOfUnitsInASubarea::getName),
                    Collectors.toCollection(TreeSet::new)
                )
            )
        );

    return subareasByArea.entrySet()
        .stream()
        .map(entry -> new AreaName(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(AreaName::getName))
        .collect(Collectors.toList());

  }

  private Instant getDateOfLatestNonProjectionBacklogPhoto(final List<VariablesPhoto> backlog, final Instant requestInstant) {
    return backlog.stream()
        .filter(stats -> !stats.isProjection())
        .map(VariablesPhoto::getDate)
        .max(Comparator.naturalOrder())
        .orElse(requestInstant);
  }

  private List<BacklogGrouper> getBacklogGroupers(final ProcessName process) {
    // only picking has areas enabled
    return process.hasAreas() ? of(PROCESS, AREA, DATE_OUT) : of(PROCESS);
  }

  private List<ProcessName> getQueryProcesses(final ProcessName process) {
    // to apply the new backlog projection we need to retrieve both waving and picking tph and backlogs
    return process == PICKING ? of(WAVING, PICKING) : of(process);
  }

  private List<VariablesPhoto> getData(final GetBacklogMonitorDetailsInput input) {
    final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());

    final List<Consolidation> currentBacklog = backlogApiAdapter.getCurrentBacklog(
        input.getRequestDate(),
        input.getWarehouseId(),
        of(input.getWorkflow()),
        getQueryProcesses(input.getProcess()),
        getBacklogGroupers(input.getProcess()),
        input.getDateFrom(),
        input.getDateTo(),
        input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS),
        input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS));

    final GetThroughputResult throughput = getThroughput(input);
    final Map<Instant, Integer> throughputByDate = getProcessThroughputByDate(throughput, input.getProcess());
    final Map<Instant, BacklogLimit> limits = getBacklogLimits(input);
    final Map<Instant, Integer> targetBacklog = getTargetBacklog(input);
    final HistoricalBacklog historicalBacklog = getHistoricalBacklog(input);
    final Map<Instant, List<NumberOfUnitsInAnArea>> historicBacklog = getPastBacklog(input, currentBacklog);
    final Map<Instant, List<NumberOfUnitsInAnArea>> projectedBacklog =
        getProjectedBacklog(input, currentBacklog, throughput.asMagnitudePhotos());

    final Map<Instant, UnitMeasure> currentBacklogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(toTotaledBacklogPhoto(historicBacklog), throughputByDate);
    final Map<Instant, UnitMeasure> projectedBacklogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(toTotaledBacklogPhoto(projectedBacklog), throughputByDate);

    return Stream.concat(
        historicBacklog.entrySet()
            .stream()
            .map(entry ->
                toProcessStats(
                    false,
                    entry.getKey(),
                    entry.getValue(),
                    targetBacklog,
                    historicalBacklog,
                    throughputByDate,
                    limits,
                    currentBacklogMeasuredInHours)),
        projectedBacklog.entrySet()
            .stream()
            .map(entry ->
                toProcessStats(
                    true,
                    entry.getKey(),
                    entry.getValue(),
                    targetBacklog,
                    historicalBacklog,
                    throughputByDate,
                    limits,
                    projectedBacklogMeasuredInHours))
    ).collect(Collectors.toList());
  }

  private VariablesPhoto toProcessStats(final boolean isProjection,
                                        final Instant date,
                                        final List<NumberOfUnitsInAnArea> areas,
                                        final Map<Instant, Integer> targetBacklog,
                                        final HistoricalBacklog historicalBacklog,
                                        final Map<Instant, Integer> throughput,
                                        final Map<Instant, BacklogLimit> limits,
                                        final Map<Instant, UnitMeasure> backlogMeasuredInHours) {

    final UnitMeasure total = backlogMeasuredInHours.getOrDefault(date, emptyMeasure());
    final Instant truncatedDate = date.truncatedTo(ChronoUnit.HOURS);
    final int tphDefault = throughput.getOrDefault(truncatedDate, 0);

    final int tphAverage = getTphAverage(total);
    final int throughputValue = tphAverage == 0 ? tphDefault : tphAverage;

    final UnitMeasure target = Optional.ofNullable(targetBacklog.get(truncatedDate))
        .map(t -> UnitMeasure.fromUnits(t, throughputValue))
        .orElse(null);

    final BacklogLimit limit = limits.get(truncatedDate);

    final UnitMeasure min = limit == null || limit.getMin() < 0 ? emptyMeasure() : fromMinutes(limit.getMin(), throughputValue);
    final UnitMeasure max = limit == null || limit.getMax() < 0 ? emptyMeasure() : fromMinutes(limit.getMax(), throughputValue);

    final Map<String, Integer> unitsByArea = areas.stream()
        .collect(
            Collectors.toMap(
                NumberOfUnitsInAnArea::getArea,
                backlog -> max(0, backlog.getUnits()),
                Integer::sum
            )
        );

    final Map<String, NumberOfUnitsInAnArea> areasByName = areas.stream()
        .collect(
            Collectors.toMap(
                NumberOfUnitsInAnArea::getArea,
                Function.identity()
            )
        );

    return new VariablesPhoto(
        isProjection,
        date,
        total,
        target,
        min,
        max,
        throughputValue,
        historicalBacklog.getOr(truncatedDate, UnitMeasure::emptyMeasure),
        unitsByArea,
        areasByName);
  }

  private boolean isCurrentProcessBacklog(final Consolidation consolidation, final ProcessName processName) {
    return Optional.of(consolidation)
        .map(Consolidation::getKeys)
        .map(keys -> keys.get("process"))
        .map(process -> process.equals(processName.getName()))
        .orElse(true);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getPastBacklog(final GetBacklogMonitorDetailsInput input,
                                                                   final List<Consolidation> currentBacklog) {

    final List<Consolidation> filteredConsolidations = currentBacklog.stream()
        .filter(consolidation -> isCurrentProcessBacklog(consolidation, input.getProcess()))
        .collect(Collectors.toList());

    final List<Consolidation> fixedConsolidation = fixBacklog(
        input.getRequestDate(),
        filteredConsolidations,
        input.getDateFrom(),
        date -> new Consolidation(date, Map.of(AREA_KEY, NO_AREA), 0, true)
    );

    return fixedConsolidation.stream()
        .collect(
            Collectors.groupingBy(
                Consolidation::getDate,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::reduceConsolidations
                )
            )
        );
  }

  private List<NumberOfUnitsInAnArea> reduceConsolidations(final List<Consolidation> consolidations) {
    final Map<String, Integer> unitsByArea = consolidations.stream()
        .collect(
            Collectors.groupingBy(
                consolidation -> consolidation.getKeys().get(AREA_KEY),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    sameAreaConsolidations -> sameAreaConsolidations.stream()
                        .mapToInt(Consolidation::getTotal)
                        .sum()
                )
            )
        );

    return unitsByArea.entrySet()
        .stream()
        .map(areaUnits -> new NumberOfUnitsInAnArea(areaUnits.getKey(), areaUnits.getValue()))
        .collect(Collectors.toList());
  }

  private List<Consolidation> fixBacklog(final Instant requestDate,
                                         final List<Consolidation> consolidation,
                                         final Instant dateFrom,
                                         final Function<Instant, Consolidation> backlogSupplier) {

    final List<Consolidation> sumsOfCellsGroupedByTakenOnDateAndProcessOnTheDot =
        filterSumsOfCellByTakenOnTheDot(consolidation);
    final Instant latestPhotoDate = getDateWhenLatestPhotoWasTaken(sumsOfCellsGroupedByTakenOnDateAndProcessOnTheDot, requestDate);
    final List<Consolidation> truncatedConsolidations = truncateToHoursTheTakenOnDate(sumsOfCellsGroupedByTakenOnDateAndProcessOnTheDot);
    return fillMissing(truncatedConsolidations, dateFrom, latestPhotoDate, backlogSupplier);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(final GetBacklogMonitorDetailsInput input,
                                                                        final List<Consolidation> backlog,
                                                                        final List<MagnitudePhoto> throughput) {

    final Instant lastConsolidationDate = getDateWhenLatestPhotoWasTaken(backlog, input.getRequestDate());

    final List<Consolidation> currentBacklog = backlog.stream()
        .filter(consolidation -> consolidation.getDate().equals(lastConsolidationDate))
        .collect(Collectors.toList());

    try {
      if (input.getProcess() == PICKING) {
        return getProjectedBacklogByArea(input, currentBacklog, throughput);
      } else {
        return getProjectedBacklogWithoutAreas(input, currentBacklog);
      }
    } catch (RuntimeException e) {
      log.error("could not retrieve backlog projections", e);
    }

    return emptyMap();
  }

  private List<NumberOfUnitsInAnArea> toUnitsInArea(final List<ProjectedBacklogForAnAreaAndOperatingHour> projections) {
    final long undefinedAreaQuantity = projections.stream()
        .filter(projection -> projection.getArea().equals("NA"))
        .mapToLong(ProjectedBacklogForAnAreaAndOperatingHour::getQuantity)
        .sum();

    final long totalUnitsInAllAreas = projections.stream()
        .mapToLong(ProjectedBacklogForAnAreaAndOperatingHour::getQuantity)
        .sum();

    final long unitsInAllValidAreas = totalUnitsInAllAreas - undefinedAreaQuantity;

    final Map<String, List<NumberOfUnitsInASubarea>> subareas = projections.stream()
        .collect(
            Collectors.groupingBy(
                projection -> projection.getArea().substring(0, 2),
                Collectors.mapping(
                    projection -> {
                      final Long thisAreaBacklog = projection.getQuantity();
                      final float undefinedAreaProportionalBacklog = (thisAreaBacklog / (float) unitsInAllValidAreas) * undefinedAreaQuantity;
                      final long thisAreaAssignedBacklog = thisAreaBacklog + (long) undefinedAreaProportionalBacklog;

                      return new NumberOfUnitsInASubarea(projection.getArea(), (int) thisAreaAssignedBacklog);
                    },
                    Collectors.toList()
                )
            )
        );

    return subareas.entrySet()
        .stream()
        .map(entry -> new NumberOfUnitsInAnArea(entry.getKey(), entry.getValue()))
        .filter(areaUnits -> !areaUnits.getArea().equals("NA"))
        .collect(Collectors.toList());
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogByArea(final GetBacklogMonitorDetailsInput input,
                                                                              final List<Consolidation> currentBacklog,
                                                                              final List<MagnitudePhoto> throughput) {

    final List<ProjectedBacklogForAnAreaAndOperatingHour> projections = projectBacklog.projectBacklogInAreas(
        input.getRequestDate(),
        input.getDateTo(),
        input.getWarehouseId(),
        input.getWorkflow(),
        List.of(WAVING, PICKING),
        currentBacklog,
        throughput
    );

    final Map<Instant, List<ProjectedBacklogForAnAreaAndOperatingHour>> groupedProjections = projections.stream()
        .filter(projection -> projection.getStatus() == BacklogProcessStatus.CARRY_OVER
            && projection.getProcess() == PICKING
            && projection.getOperatingHour().isAfter(input.getRequestDate())
        )
        .collect(
            Collectors.groupingBy(
                ProjectedBacklogForAnAreaAndOperatingHour::getOperatingHour,
                Collectors.toList()
            )
        );

    Map<Instant, List<NumberOfUnitsInAnArea>> mapBacklogs=  groupedProjections.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> toUnitsInArea(entry.getValue())
        ));

    final Map<Instant, List<ProjectedBacklogForAnAreaAndOperatingHour>> groupedProjectionsProcessed = projections.stream()
        .filter(projection -> projection.getStatus() == BacklogProcessStatus.PROCESSED
            && projection.getProcess() == PICKING
            && projection.getOperatingHour().isAfter(input.getRequestDate()))
        .collect(
            Collectors.groupingBy(
                ProjectedBacklogForAnAreaAndOperatingHour::getOperatingHour,
                Collectors.toList()
            )
        );

    Map<Instant, List<NumberOfUnitsInAnArea>> mapBacklogsProcessed=  groupedProjectionsProcessed.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> toUnitsInArea(entry.getValue())
        ));

    Map<Instant, List<HeadCountByArea>> projectionHeadcount =  getProjectionHeadcount.getProjectionHeadcount(input.getWarehouseId(), mapBacklogsProcessed);


    return assignHeadcount(mapBacklogs, projectionHeadcount);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> assignHeadcount(Map<Instant, List<NumberOfUnitsInAnArea>> backlogs, Map<Instant, List<HeadCountByArea>> suggestedHeadCount){

    return backlogs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->{

      List<HeadCountByArea> headCountByAreaList = suggestedHeadCount.get(entry.getKey());
      return  entry.getValue()
          .stream()
          .map(value -> {
            HeadCountByArea headCountByArea = headCountByAreaList.stream()
                .filter(headCount -> headCount.getArea().equals(value.getArea()))
                .findAny()
                .orElseGet(() -> new HeadCountByArea(null, null, null, Collections.emptyList()));

            return new NumberOfUnitsInAnArea(value.getArea(),
                assignSubareas(value.getSubareas(), headCountByArea.getSubAreas()),
                headCountByArea.getReps(),
                headCountByArea.getRespPercentage());
          })
          .collect(Collectors.toList());
    }));

  }

  private List<NumberOfUnitsInAnArea.NumberOfUnitsInASubarea> assignSubareas(List<NumberOfUnitsInAnArea.NumberOfUnitsInASubarea> subareas, List<HeadcountBySubArea> subAreasHeadcount){
    return subareas
        .stream()
        .map(value -> {
          HeadcountBySubArea headcountBySubArea = subAreasHeadcount
              .stream()
              .filter(subArea -> subArea.getSubArea().equals(value.getName()))
              .findAny()
              .orElseGet(() -> new HeadcountBySubArea(null,null,null));

          return new NumberOfUnitsInAnArea.NumberOfUnitsInASubarea(value.getName(),
              value.getUnits(),
              headcountBySubArea.getReps(),
              headcountBySubArea.getRespPercentage());
        })
        .collect(Collectors.toList());
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogWithoutAreas(final GetBacklogMonitorDetailsInput input,
                                                                                    final List<Consolidation> currentBacklog) {

    final Instant dateFrom = input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

    final Instant dateTo = input.getDateTo()
        .truncatedTo(ChronoUnit.HOURS)
        .minus(1L, ChronoUnit.HOURS);

    final List<BacklogProjectionResponse> projectedBacklog = backlogApiAdapter
        .getProjectedBacklog(
            input.getWarehouseId(),
            input.getWorkflow(),
            PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
            dateFrom.atZone(UTC),
            dateTo.atZone(UTC),
            input.getCallerId(),
            currentBacklog);

    return projectedBacklog.stream()
        .filter(projection -> projection.getProcessName().equals(input.getProcess()))
        .findFirst()
        .map(projection -> projection.getValues()
            .stream()
            .collect(Collectors.toMap(
                projectionValue -> projectionValue.getDate().toInstant(),
                projectionValue -> of(
                    new NumberOfUnitsInAnArea(
                        NO_AREA,
                        projectionValue.getQuantity()))))).orElseGet(Collections::emptyMap);
  }

  private Map<Instant, Integer> getTargetBacklog(final GetBacklogMonitorDetailsInput input) {

    if (!input.getProcess().hasTargetBacklog()) {
      return Collections.emptyMap();
    }

    final TrajectoriesRequest request = TrajectoriesRequest.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .processName(of(input.getProcess()))
        /* Note that the zone is not necessary but the PlanningModelGateway requires it to
        no avail. */
        .dateFrom(ZonedDateTime.ofInstant(input.getDateFrom(), UTC))
        .dateTo(ZonedDateTime.ofInstant(input.getDateTo(), UTC))
        .source(FORECAST)
        .build();

    return planningModelGateway.getPerformedProcessing(request)
        .stream().collect(
            Collectors.toMap(
                entity -> entity.getDate().toInstant(),
                MagnitudePhoto::getValue));
  }

  private GetThroughputResult getThroughput(final GetBacklogMonitorDetailsInput input) {
    final GetThroughputInput request = GetThroughputInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processes(getQueryProcesses(input.getProcess()))
        .dateFrom(input.getDateFrom().atZone(UTC))
        .dateTo(input.getDateTo().atZone(UTC).plusHours(HOUR_TPH_FUTURE))
        .build();

    try {
      return getProcessThroughput.execute(request);
    } catch (RuntimeException e) {
      log.error("could not retrieve throughput for {}", request, e);
    }
    return GetThroughputResult.emptyThroughput();
  }

  private Map<Instant, Integer> getProcessThroughputByDate(final GetThroughputResult result, final ProcessName process) {
    return result.getOrDefault(process, Map.of())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey().toInstant(), Map.Entry::getValue));
  }

  private HistoricalBacklog getHistoricalBacklog(final GetBacklogMonitorDetailsInput input) {
    return getHistoricalBacklog.execute(
        new GetHistoricalBacklogInput(
            input.getRequestDate(),
            input.getWarehouseId(),
            input.getWorkflow(),
            of(input.getProcess()),
            input.getDateFrom(),
            input.getDateTo())).get(input.getProcess());
  }

  private Map<Instant, BacklogLimit> getBacklogLimits(final GetBacklogMonitorDetailsInput input) {
    try {
      return getBacklogLimits.execute(
              GetBacklogLimitsInput.builder()
                  .warehouseId(input.getWarehouseId())
                  .workflow(input.getWorkflow())
                  .processes(of(input.getProcess()))
                  .dateFrom(input.getDateFrom())
                  .dateTo(input.getDateTo())
                  .build()
          )
          .getOrDefault(input.getProcess(), emptyMap());

    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return emptyMap();
  }

  private List<DetailedBacklogPhoto> getBacklogDetails(final List<VariablesPhoto> backlog,
                                                       final List<AreaName> areas,
                                                       final Instant currentDatetime) {

    return backlog.stream()
        .map(b -> this.toProcessDetail(b, areas, currentDatetime))
        .sorted(comparing(DetailedBacklogPhoto::getDate))
        .collect(Collectors.toList());
  }

  private ProcessDetail getResumedBacklog(final ProcessName process,
                                          final Instant currentDatetime,
                                          final List<VariablesPhoto> variablesTrajectory) {

    return build(
        process,
        currentDatetime,
        variablesTrajectory.stream()
            .map(current -> new BacklogStatsByDate(
                current.getDate(),
                current.getTotal(),
                current.getHistorical(),
                current.getMinLimit(),
                current.getMaxLimit())).collect(Collectors.toList()));
  }

  private DetailedBacklogPhoto toProcessDetail(final VariablesPhoto variablesPhoto,
                                               final List<AreaName> processAreas,
                                               final Instant currentDatetime) {

    final UnitMeasure totalBacklog = variablesPhoto.getTotal();
    final UnitMeasure targetBacklog = variablesPhoto.getTarget();
    final Integer totalReps = variablesPhoto.getAreas()
        .values()
        .stream()
        .map(NumberOfUnitsInAnArea::getReps)
        .filter(Objects::nonNull)
        .reduce(0, Integer::sum);

    final Headcount headcount = new Headcount(totalReps,1.0);

    final List<AreaBacklogDetail> areas = processAreas.isEmpty()
        ? null
        : toAreas(variablesPhoto, processAreas);

    final Instant date = variablesPhoto.getDate().equals(currentDatetime)
        ? currentDatetime
        : variablesPhoto.getDate().truncatedTo(ChronoUnit.HOURS);

    return new DetailedBacklogPhoto(date,headcount, targetBacklog, totalBacklog, areas);
  }

  private AreaBacklogDetail mapBacklogAreaDetail(final AreaName area, final NumberOfUnitsInAnArea unitsInThisArea,
                                                 final Integer throughput) {
    final Optional<NumberOfUnitsInAnArea> numberOfUnitsInAnArea = Optional.ofNullable(unitsInThisArea);

    final List<SubAreaBacklogDetail> mappedSubareas = area.getSubareas()
        .stream()
        .map(subarea -> {
              final Optional<NumberOfUnitsInASubarea> unitsInSubArea = numberOfUnitsInAnArea.flatMap(subareas -> subareas.getSubareas()
                  .stream()
                  .filter(s -> s.getName().equals(subarea))
                  .findFirst()
              );

              return unitsInSubArea.map(value -> new SubAreaBacklogDetail(subarea, UnitMeasure.fromUnits(value.getUnits(), throughput), new Headcount(value.getReps(), value.getRepsPercentage())))
                  .orElseGet(() -> new SubAreaBacklogDetail(subarea, UnitMeasure.emptyMeasure(), new Headcount(0,0.0)));
            }
        )
        .collect(Collectors.toList());


    final Integer totalUnitsInArea = numberOfUnitsInAnArea.map(NumberOfUnitsInAnArea::getUnits).orElse(0);
    final UnitMeasure measure = UnitMeasure.fromUnits(totalUnitsInArea, throughput);

    Headcount headcountArea = numberOfUnitsInAnArea.stream()
        .map(value-> new Headcount(value.getUnits() != null ? value.getUnits():0,value.getRepsPercentage()!= null? value.getRepsPercentage(): 0D))
        .findAny()
        .orElse(new Headcount(0,0D));

    return new AreaBacklogDetail(area.getName(), measure, headcountArea, mappedSubareas);
  }

  private List<AreaBacklogDetail> toAreas(final VariablesPhoto variablesPhoto, final List<AreaName> areas) {
    final Integer throughput = variablesPhoto.getThroughput();
    final Map<String, NumberOfUnitsInAnArea> currentPhotoAreas = variablesPhoto.getAreas();

    return areas.stream()
        .map(area -> mapBacklogAreaDetail(area, currentPhotoAreas.get(area.getName()), throughput))
        .collect(Collectors.toList());
  }

  private List<TotaledBacklogPhoto> toTotaledBacklogPhoto(final Map<Instant, List<NumberOfUnitsInAnArea>> totaledBacklogByArea) {

    return totaledBacklogByArea.entrySet().stream()
        .map(entry -> new TotaledBacklogPhoto(entry.getKey(), entry.getValue().stream().map(NumberOfUnitsInAnArea::getUnits)
            .reduce(0, Integer::sum))).collect(Collectors.toList());
  }

  private int getTphAverage(final UnitMeasure backlogMeasuredInHour) {
    double avgByHour = (double) backlogMeasuredInHour.getMinutes() / 60;
    return avgByHour > 0 ? (int) ((double) backlogMeasuredInHour.getUnits() / avgByHour) : 0;
  }

  /**
   * Remembers the value that some backlog related variables have at some instant.
   */
  @Value
  private static class VariablesPhoto {
    /**
     * Tells if this photo is taken in the future. In that case the variables values comes from
     * forecasts.
     */
    boolean isProjection;

    /**
     * The instant when the photo is taken (pass or future).
     */
    Instant date;

    /**
     * The total backlog.
     */
    UnitMeasure total;

    /**
     * The desired backlog.
     */
    UnitMeasure target;

    /**
     * The minimum value the backlog should not break through at that instant.
     */
    UnitMeasure minLimit;

    /**
     * The maximum value the backlog should not break through at that instant.
     */
    UnitMeasure maxLimit;

    /**
     * The number of units processed per hour at that instant.
     */
    Integer throughput;

    /**
     * The total backlog at the same instant of the previous week.
     */
    UnitMeasure historical;

    /**
     * The total backlog broken down by area at that instant.
     */
    Map<String, Integer> unitsByArea;

    Map<String, NumberOfUnitsInAnArea> areas;
  }

  @Value
  private static class AreaName {
    String name;

    Set<String> subareas;
  }
}
