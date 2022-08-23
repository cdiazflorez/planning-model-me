package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.emptyMeasure;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromMinutes;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog.emptyBacklog;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.naturalOrder;

import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitor extends GetConsolidatedBacklog {

  private static final Map<Workflow, String> GROUP_TYPE_BY_WORKFLOW = Map.of(
      FBM_WMS_OUTBOUND, "order",
      FBM_WMS_INBOUND, ""
  );

  private static final int HOUR_TPH_FUTURE = 24;

  private final ProjectBacklog backlogProjection;

  private final BacklogPhotoApiGateway backlogPhotoApiAdapter;

  private final GetProcessThroughput getProcessThroughput;

  private final GetHistoricalBacklog getHistoricalBacklog;

  private final GetBacklogLimits getBacklogLimits;

  private static int getTphAveragePerHour(final UnitMeasure backlogMeasuredInHour) {
    final int seconds = 60;

    double avgByHour = (double) backlogMeasuredInHour.getMinutes() / seconds;

    return avgByHour > 0 ? (int) ((double) backlogMeasuredInHour.getUnits() / avgByHour) : 0;
  }

  /**
   * Get backlog monitor
   * Gets details for summary of monitor.
   *
   * @param input parameter for to gets details of summary monitor
   * @return workflow Backlog Detail of summary monitor
   */
  public WorkflowBacklogDetail execute(final GetBacklogMonitorInputDto input) {
    final List<ProcessData> processData = getData(input);
    final Instant takenOnDateOfLastPhoto = getDateWhenLatestPhotoOfAllCurrentBacklogsWasTaken(
        processData,
        input.getRequestDate().truncatedTo(ChronoUnit.SECONDS)
    );

    return new WorkflowBacklogDetail(
        input.getWorkflow().getName(),
        takenOnDateOfLastPhoto,
        buildProcesses(processData, takenOnDateOfLastPhoto)
    );
  }

  private List<ProcessData> getData(final GetBacklogMonitorInputDto input) {
    final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());

    final Map<ProcessName, List<BacklogPhoto>> backlogPhotoByProcess = backlogPhotoApiAdapter.getTotalBacklogPerProcessAndInstantDate(
        new BacklogRequest(
            input.getWarehouseId(),
            Set.of(input.getWorkflow()),
            Set.copyOf(input.getProcesses()),
            input.getDateFrom(),
            input.getRequestDate().truncatedTo(ChronoUnit.SECONDS),
            null,
            null,
            input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS),
            input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS),
            Set.of(STEP, AREA)
        )
    );

    final Map<ProcessName, List<BacklogPhoto>> projectedBacklog = getProjectedBacklog(workflow, input, backlogPhotoByProcess);
    final Map<ProcessName, HistoricalBacklog> historicalBacklog = getHistoricalBacklog(input);
    final Map<ProcessName, Map<Instant, BacklogLimit>> backlogLimits = getBacklogLimits(input);

    final GetThroughputResult throughput = getThroughput(input);

    return input.getProcesses().stream()
        .map(processName -> new ProcessData(
            ProcessName.from(processName.getName()),
            backlogPhotoByProcess.getOrDefault(processName, emptyList()),
            projectedBacklog.getOrDefault(processName, emptyList()),
            historicalBacklog.getOrDefault(processName, emptyBacklog()),
            throughput.find(ProcessName.from(processName.getName())).orElse(emptyMap()),
            backlogLimits.getOrDefault(ProcessName.from(processName.getName()), emptyMap())
        ))
        .collect(Collectors.toList());
  }

  private Map<ProcessName, HistoricalBacklog> getHistoricalBacklog(final GetBacklogMonitorInputDto input) {
    try {
      return getHistoricalBacklog.execute(new GetHistoricalBacklogInput(
          input.getRequestDate(),
          input.getWarehouseId(),
          input.getWorkflow(),
          input.getProcesses(),
          input.getDateFrom(),
          input.getDateTo()));

    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return emptyMap();
  }

  private Map<ProcessName, List<BacklogPhoto>> getProjectedBacklog(
      final BacklogWorkflow workflow,
      final GetBacklogMonitorInputDto input,
      final Map<ProcessName, List<BacklogPhoto>> backlogPhotoByProcess) {

    try {
      /* The zone is not necessary but the ProjectBacklog use case requires it to no avail. */
      final ZonedDateTime requestDate = ZonedDateTime.ofInstant(input.getRequestDate().truncatedTo(ChronoUnit.HOURS), UTC);

      var currentBacklog = backlogPhotoByProcess.entrySet().stream()
          .map(backlogByProcess ->
                   new CurrentBacklog(backlogByProcess.getKey(),
                                      !backlogByProcess.getValue().isEmpty() ? backlogByProcess.getValue().get(0).getQuantity() : 0))
          .collect(Collectors.toList());

      var backlogProjectionInput = BacklogProjectionInput.builder()
          .workflow(input.getWorkflow())
          .warehouseId(input.getWarehouseId())
          .processName(input.getProcesses())
          .slaDateFrom(input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS))
          .slaDateTo(input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS))
          .dateFrom(requestDate)
          .dateTo(ZonedDateTime.ofInstant(input.getDateTo(), UTC))
          .groupType(GROUP_TYPE_BY_WORKFLOW.get(input.getWorkflow()))
          .userId(input.getCallerId())
          .backlogs(currentBacklog)
          .backlogPhotoByProcess(backlogPhotoByProcess)
          .hasWall(input.isHasWall())
          .build();

      final List<BacklogProjectionResponse> projectedBacklog = backlogProjection.execute(backlogProjectionInput);

      return projectedBacklog.stream()
          .collect(Collectors.groupingBy(
              BacklogProjectionResponse::getProcessName,
              Collectors.flatMapping(p -> p.getValues()
                                         .stream()
                                         .filter(v -> !v.getDate()
                                             .toInstant()
                                             .isAfter(input.getDateTo())
                                         )
                                         .map(v -> new BacklogPhoto(
                                             v.getDate().toInstant(),
                                             v.getQuantity())),
                                     Collectors.toList())));

    } catch (RuntimeException e) {
      log.error("could not retrieve backlog projections", e);
    }
    return emptyMap();
  }

  private GetThroughputResult getThroughput(final GetBacklogMonitorInputDto input) {

    final GetThroughputInput request = GetThroughputInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processes(input.getProcesses())
        /* Note that the zone is not necessary but the GetProcessThroughput use case
        requires it to no avail. */
        .dateFrom(ZonedDateTime.ofInstant(input.getDateFrom(), UTC))
        .dateTo(ZonedDateTime.ofInstant(input.getDateTo(), UTC).plusHours(HOUR_TPH_FUTURE))
        .build();

    try {
      return getProcessThroughput.execute(request);
    } catch (RuntimeException e) {
      log.error("could not retrieve throughput for {}", request, e);
      return GetThroughputResult.emptyThroughput();
    }
  }

  private Map<ProcessName, Map<Instant, BacklogLimit>> getBacklogLimits(final GetBacklogMonitorInputDto input) {
    try {
      return getBacklogLimits.execute(
          GetBacklogLimitsInput.builder()
              .warehouseId(input.getWarehouseId())
              .workflow(input.getWorkflow())
              .processes(input.getProcesses())
              .dateFrom(input.getDateFrom())
              .dateTo(input.getDateTo())
              .build());

    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return emptyMap();
  }

  private List<ProcessDetail> buildProcesses(final List<ProcessData> data,
                                             final Instant currentDateTime) {

    return data.stream()
        .map(detail -> build(
            detail.getProcess(),
            currentDateTime,
            toProcessDescription(detail)))
        .collect(Collectors.toList());
  }

  private List<BacklogStatsByDate> toProcessDescription(final ProcessData data) {
    final Map<Instant, Integer> throughput = data.getThroughputByDate();
    final HistoricalBacklog historical = data.getHistoricalBacklog();
    final Map<Instant, BacklogLimit> limits = data.getBacklogLimits();

    final Map<Instant, UnitMeasure> currentBacklogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(emptyList(), data.getCurrentBacklog(), throughput);
    final Map<Instant, UnitMeasure> projectedBacklogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(emptyList(), data.getProjectedBacklog(), throughput);

    return Stream.concat(
        toBacklogStatsByDate(data.getCurrentBacklog(), throughput, historical, limits, currentBacklogMeasuredInHours),
        toBacklogStatsByDate(data.getProjectedBacklog(), throughput, historical, limits, projectedBacklogMeasuredInHours)
    ).collect(Collectors.toList());
  }

  private Stream<BacklogStatsByDate> toBacklogStatsByDate(
      final List<BacklogPhoto> backlogPhotos,
      final Map<Instant, Integer> throughputByHour,
      final HistoricalBacklog historical,
      final Map<Instant, BacklogLimit> limits,
      final Map<Instant, UnitMeasure> backlogMeasuredInHours
  ) {
    return backlogPhotos.stream()
        .map(photo -> {
          final Instant truncatedDateOfPhoto = photo.getTakenOn().truncatedTo(ChronoUnit.HOURS);
          final int tphDefault = throughputByHour.getOrDefault(truncatedDateOfPhoto, 0);
          final UnitMeasure total = backlogMeasuredInHours.getOrDefault(photo.getTakenOn(), emptyMeasure());
          final BacklogLimit limit = limits.get(truncatedDateOfPhoto);

          final int tphAverage = getTphAveragePerHour(total);
          final int tph = tphAverage == 0 ? tphDefault : tphAverage;

          final UnitMeasure min = limit == null || limit.getMin() < 0 ? emptyMeasure() : fromMinutes(limit.getMin(), tph);
          final UnitMeasure max = limit == null || limit.getMax() < 0 ? emptyMeasure() : fromMinutes(limit.getMax(), tph);

          final UnitMeasure average = historical.getOr(truncatedDateOfPhoto, UnitMeasure::emptyMeasure);

          return new BacklogStatsByDate(
              photo.getTakenOn(),
              total,
              average,
              min,
              max
          );
        });
  }

  /**
   * FIXME this method is similar to
   * {@link GetConsolidatedBacklog#getDateWhenLatestPhotoWasTaken(List, Instant)}. Low cohesion
   * detected!
   */
  private Instant getDateWhenLatestPhotoOfAllCurrentBacklogsWasTaken(
      final List<ProcessData> processesData,
      final Instant defaultDate) {
    return processesData.stream()
        .flatMap(p -> p.getCurrentBacklog()
            .stream()
            .map(BacklogPhoto::getTakenOn))
        .max(naturalOrder())
        .orElse(defaultDate);
  }

  @Value
  private static class ProcessData {
    ProcessName process;

    List<BacklogPhoto> currentBacklog;

    List<BacklogPhoto> projectedBacklog;

    HistoricalBacklog historicalBacklog;

    Map<Instant, Integer> throughputByDate;

    Map<Instant, BacklogLimit> backlogLimits;
  }
}
