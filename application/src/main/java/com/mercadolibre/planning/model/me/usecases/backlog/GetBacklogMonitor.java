package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.emptyMeasure;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromMinutes;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromUnits;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog.emptyBacklog;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.naturalOrder;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitor extends GetConsolidatedBacklog {

    private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
            FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING),
            FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
    );

    private final BacklogApiAdapter backlogApiAdapter;

    private final GetProcessThroughput getProcessThroughput;

    private final GetHistoricalBacklog getHistoricalBacklog;

    private final GetBacklogLimits getBacklogLimits;

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

        final List<Consolidation> currentBacklogBase =
                backlogApiAdapter.getCurrentBacklog(
                        input.getRequestDate(),
                        input.getWarehouseId(),
                        of(input.getWorkflow()),
                        PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
                        input.getDateFrom(),
                        input.getRequestDate().truncatedTo(ChronoUnit.SECONDS));

        final Map<ProcessName, List<TotaledBacklogPhoto>> currentBacklog = getCurrentBacklog(input, currentBacklogBase);
        final Map<ProcessName, List<TotaledBacklogPhoto>> projectedBacklog = getProjectedBacklog(input, currentBacklogBase);
        final Map<ProcessName, HistoricalBacklog> historicalBacklog = getHistoricalBacklog(input);
        final Map<ProcessName, Map<Instant, BacklogLimit>> backlogLimits = getBacklogLimits(input);

        final GetThroughputResult throughput = getThroughput(input);

        return PROCESS_BY_WORKFLOWS.get(input.getWorkflow())
                .stream()
                .map(processName -> new ProcessData(
                        processName,
                        currentBacklog.getOrDefault(processName, emptyList()),
                        projectedBacklog.getOrDefault(processName, emptyList()),
                        historicalBacklog.getOrDefault(processName, emptyBacklog()),
                        throughput.find(processName).orElse(emptyMap()),
                        backlogLimits.getOrDefault(processName, emptyMap())
                ))
                .collect(Collectors.toList());
    }

    private Map<ProcessName, List<TotaledBacklogPhoto>> getCurrentBacklog(final GetBacklogMonitorInputDto input,
                                                                          final List<Consolidation> cellsGroupedByTakenOnAndProcessName) {
        /* Get the backlog photos taken between `dateFrom` and `dateTo`, consolidating all the cells
        corresponding to the same photo and process. */
        final Map<ProcessName, List<Consolidation>> consolidationsTrajectoriesByProcess =
                groupBacklogSubsetsByProcess(
                    input.getRequestDate(),
                    cellsGroupedByTakenOnAndProcessName,
                    PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
                    input.getDateFrom());

        return consolidationsTrajectoriesByProcess.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()
                                .stream()
                                .map(b -> new TotaledBacklogPhoto(b.getDate(), b.getTotal()))
                                .collect(Collectors.toList())));
    }

    /**
     * Groups the received instances of {@link Consolidation} by process name.
     * Also, truncates to hours the date at which the photos were taken on, excepting the last
     * photo.
     * @param requestDate the moment when the request was received
     * @param sumsOfCellsGroupedByTakenOnDateAndProcess the backlog photos taken between `dateFrom`
     *     and `requestInstant`, with all the cells corresponding to the same photo and process
     *     consolidated.
     * @param processes the names of the processes to include in the result
     * @param dateFrom the lower bound of the
     * */
    private Map<ProcessName, List<Consolidation>> groupBacklogSubsetsByProcess(
            final Instant requestDate,
            final List<Consolidation> sumsOfCellsGroupedByTakenOnDateAndProcess,
            final List<ProcessName> processes,
            final Instant dateFrom
    ) {
        final Instant takenOnDateOfLastPhoto = getDateWhenLatestPhotoWasTaken(
                sumsOfCellsGroupedByTakenOnDateAndProcess,
                requestDate.truncatedTo(ChronoUnit.SECONDS));

        final List<Consolidation>
                truncatedConsolidation = truncateToHoursTheTakenOnDatesExceptFor(
                        sumsOfCellsGroupedByTakenOnDateAndProcess,
                        takenOnDateOfLastPhoto);

        final Map<ProcessName, List<Consolidation>> backlogByProcess =
                truncatedConsolidation.stream()
                    .collect(Collectors.groupingBy(
                            this::processNameFromBacklog,
                            Collectors.toList()));

        return processes.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        process ->
                                fillMissing(
                                        backlogByProcess.getOrDefault(process, emptyList()),
                                        dateFrom,
                                        takenOnDateOfLastPhoto,
                                        date -> new Consolidation(date, null, 0))));
    }

    private Map<ProcessName, HistoricalBacklog> getHistoricalBacklog(final GetBacklogMonitorInputDto input) {

        try {
            return getHistoricalBacklog.execute(
                    new GetHistoricalBacklogInput(
                            input.getRequestDate(),
                            input.getWarehouseId(),
                            of(input.getWorkflow()),
                            PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
                            input.getDateFrom(),
                            input.getDateTo()));

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return emptyMap();
    }

    private Map<ProcessName, List<TotaledBacklogPhoto>> getProjectedBacklog(
            final GetBacklogMonitorInputDto input,
            final List<Consolidation> currentBacklog) {

        try {
            /* The zone is not necessary but the ProjectBacklog use case requires it to no avail. */
            final ZonedDateTime requestDate = ZonedDateTime.ofInstant(input.getRequestDate(), UTC);

            final List<BacklogProjectionResponse> projectedBacklog = backlogApiAdapter
                    .getProjectedBacklog(
                            input.getWarehouseId(),
                            input.getWorkflow(),
                            PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
                            requestDate,
                            requestDate.plusHours(25),
                            input.getCallerId(),
                            currentBacklog);

            return projectedBacklog.stream()
                    .collect(Collectors.groupingBy(
                            BacklogProjectionResponse::getProcessName,
                            Collectors.flatMapping(p -> p.getValues()
                                            .stream()
                                            .filter(v -> !v.getDate()
                                                    .toInstant()
                                                    .isAfter(input.getDateTo())
                                            )
                                            .map(v -> new TotaledBacklogPhoto(
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
                .processes(PROCESS_BY_WORKFLOWS.get(input.getWorkflow()))
                /* Note that the zone is not necessary but the GetProcessThroughput use case
                requires it to no avail. */
                .dateFrom(ZonedDateTime.ofInstant(input.getDateFrom(), UTC))
                .dateTo(ZonedDateTime.ofInstant(input.getDateTo(), UTC))
                .build();

        try {
            return getProcessThroughput.execute(request);
        } catch (RuntimeException e) {
            log.error("could not retrieve throughput for {}", request, e);
        }
        return GetThroughputResult.emptyThroughput();
    }

    private Map<ProcessName, Map<Instant, BacklogLimit>> getBacklogLimits(
            final GetBacklogMonitorInputDto input) {

        try {
            return getBacklogLimits.execute(
                    GetBacklogLimitsInput.builder()
                            .warehouseId(input.getWarehouseId())
                            .workflow(input.getWorkflow())
                            .processes(PROCESS_BY_WORKFLOWS.get(input.getWorkflow()))
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

        return Stream.concat(
                toBacklogStatsByDate(data.getCurrentBacklog(), throughput, historical, limits),
                toBacklogStatsByDate(data.getProjectedBacklog(), throughput, historical, limits)
        ).collect(Collectors.toList());
    }

    private Stream<BacklogStatsByDate> toBacklogStatsByDate(
            final List<TotaledBacklogPhoto> totaledBacklogPhotos,
            final Map<Instant, Integer> throughputByHour,
            final HistoricalBacklog historical,
            final Map<Instant, BacklogLimit> limits
    ) {
        return totaledBacklogPhotos.stream()
                .map(photo -> {
                    final Instant truncatedDateOfPhoto =
                            photo.getTakenOn().truncatedTo(ChronoUnit.HOURS);
                    final Integer tph = throughputByHour.get(truncatedDateOfPhoto);
                    final BacklogLimit limit = limits.get(truncatedDateOfPhoto);

                    final UnitMeasure total = fromUnits(photo.getQuantity(), tph);

                    final UnitMeasure min = limit == null || limit.getMin() < 0
                            ? emptyMeasure() : fromMinutes(limit.getMin(), tph);

                    final UnitMeasure max = limit == null || limit.getMax() < 0
                            ? emptyMeasure() : fromMinutes(limit.getMax(), tph);

                    final UnitMeasure average = historical
                            .getOr(truncatedDateOfPhoto, UnitMeasure::emptyMeasure);

                    return new BacklogStatsByDate(
                            photo.getTakenOn(),
                            total,
                            average,
                            min,
                            max
                    );
                });
    }

    private ProcessName processNameFromBacklog(final Consolidation b) {
        return ProcessName.from(b.getKeys().get("process"));
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
                        .map(TotaledBacklogPhoto::getTakenOn))
                .max(naturalOrder())
                .orElse(defaultDate);
    }

    @Value
    private static class ProcessData {
        ProcessName process;
        List<TotaledBacklogPhoto> currentBacklog;
        List<TotaledBacklogPhoto> projectedBacklog;
        HistoricalBacklog historicalBacklog;
        Map<Instant, Integer> throughputByDate;
        Map<Instant, BacklogLimit> backlogLimits;
    }
}
