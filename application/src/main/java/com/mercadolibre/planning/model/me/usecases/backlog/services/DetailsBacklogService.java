package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.NO_AREA;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.mergeMaps;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.selectPhotos;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.BacklogWorkflow;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class DetailsBacklogService implements GetBacklogMonitorDetails.BacklogProvider {

  protected static final Map<Workflow, List<ProcessName>> PROCESS_DEPENDENCIES_BY_WORKFLOWS = Map.of(
      FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL),
      FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
  );
  private static final List<ProcessName> PROCESSES = of(WAVING, WALL_IN, PACKING_WALL, CHECK_IN, PUT_AWAY);

  private static final Duration DEFAULT_HOURS_LOOKBACK = Duration.ofHours(2);

  protected final ProjectionGateway projectionGateway;
  private final BacklogPhotoApiGateway backlogPhotoApiGateway;

  @Override
  public boolean canProvide(final ProcessName process) {
    return PROCESSES.contains(process);
  }

  @Override
  public Map<Instant, List<NumberOfUnitsInAnArea>> getMonitorBacklog(final BacklogProviderInput input) {
    final var currentBacklog = getCurrentBacklog(input);
    final var backlogByProcess = getCurrentBacklogForAllProcess(currentBacklog);
    final var mappedBacklog = mapBacklogToNumberOfUnitsInAreasByTakenOn(currentBacklog.get(input.getProcess()));

    final var pastBacklog = selectPhotos(mappedBacklog, input.getDateFrom(), input.getRequestDate());
    final var projectedBacklog = getProjectedBacklog(input, backlogByProcess);

    return mergeMaps(pastBacklog, projectedBacklog);
  }

  private List<CurrentBacklog> getCurrentBacklogForAllProcess(Map<ProcessName, List<BacklogPhoto>> backlogPhotoByProcess) {

    return backlogPhotoByProcess.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map(backlogByProcess ->
            new CurrentBacklog(backlogByProcess.getKey(),
                !backlogByProcess.getValue().isEmpty()
                    ? backlogByProcess.getValue().stream().max(Comparator.comparing(BacklogPhoto::getTakenOn)).get().getQuantity()
                    : 0
            )
        )
        .collect(Collectors.toList());
  }

  private Map<ProcessName, List<BacklogPhoto>> getCurrentBacklog(final BacklogProviderInput input) {
    final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());
    return backlogPhotoApiGateway.getTotalBacklogPerProcessAndInstantDate(
        new BacklogRequest(
            input.getWarehouseId(),
            Set.of(input.getWorkflow()),
            Set.copyOf(PROCESS_DEPENDENCIES_BY_WORKFLOWS.get(input.getWorkflow())),
            input.getRequestDate().truncatedTo(ChronoUnit.HOURS).minus(DEFAULT_HOURS_LOOKBACK),
            input.getRequestDate().truncatedTo(ChronoUnit.SECONDS),
            null,
            null,
            input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS),
            input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS),
            Set.of(STEP, AREA)
        ),
        false
    );
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> mapBacklogToNumberOfUnitsInAreasByTakenOn(final List<BacklogPhoto> backlog) {
    return backlog.stream()
        .collect(Collectors.toMap(
            BacklogPhoto::getTakenOn,
            photo -> of(
                new NumberOfUnitsInAnArea(photo.getGroupValue(AREA).orElse(NO_AREA), photo.getQuantity())
            )
        ));
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(final BacklogProviderInput input,
                                                                        final List<CurrentBacklog> currentBacklog) {

    try {
      return getProjectedBacklogWithoutAreas(input, currentBacklog);
    } catch (RuntimeException e) {
      log.error("could not retrieve backlog projections", e);
    }

    return emptyMap();
  }

  protected Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogWithoutAreas(final BacklogProviderInput input,
                                                                                      final List<CurrentBacklog> currentBacklog) {

    final Instant dateFrom = input.getDateFrom().isAfter(input.getRequestDate())
        ? input.getDateFrom().truncatedTo(ChronoUnit.HOURS)
        : input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

    final Instant dateTo = input.getDateTo()
        .truncatedTo(ChronoUnit.HOURS);

    final List<BacklogProjectionResponse> projectedBacklog = projectionGateway.getBacklogProjection(
        BacklogProjectionRequest.builder()
            .warehouseId(input.getWarehouseId())
            .workflow(input.getWorkflow())
            .processName(PROCESS_DEPENDENCIES_BY_WORKFLOWS.get(input.getWorkflow()))
            .dateFrom(dateFrom.atZone(UTC))
            .dateTo(dateTo.atZone(UTC))
            .currentBacklog(currentBacklog)
            .applyDeviation(true)
            .packingWallRatios(getPackingWallRatios(input))
            .build()
    );

    return projectedBacklog.stream()
        .filter(projection -> projection.getProcessName().getName().equalsIgnoreCase(input.getProcess().getName()))
        .findFirst()
        .map(projection -> projection.getValues()
            .stream()
            .collect(
                Collectors.toMap(
                    projectionValue -> projectionValue.getDate().toInstant(),
                    projectionValue -> of(
                        new NumberOfUnitsInAnArea(
                            NO_AREA,
                            projectionValue.getQuantity()
                        )
                    )
                ))
        )
        .orElseGet(Collections::emptyMap);
  }

  protected Map<Instant, Double> getPackingWallRatios(final BacklogProviderInput input) {
    return emptyMap();
  }
}
