package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog.emptyBacklog;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.naturalOrder;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitor extends GetConsolidatedBacklog {
    private static final long BACKLOG_WEEKS_LOOKBACK = 3L;

    private static final String OUTBOUND_ORDERS = "outbound-orders";

    private static final Map<String, List<ProcessName>> WORKFLOWS = Map.of(
            OUTBOUND_ORDERS, of(WAVING, PICKING, PACKING)
    );

    private final BacklogApiGateway backlogApiGateway;

    private final ProjectBacklog backlogProjection;

    private final GetProcessThroughput getProcessThroughput;

    private final GetHistoricalBacklog getHistoricalBacklog;

    public WorkflowBacklogDetail execute(GetBacklogMonitorInputDto input) {
        final List<ProcessData> processData = getData(input);
        final ZonedDateTime currentDateTime = getCurrentDatetime(processData);

        return new WorkflowBacklogDetail(
                input.getWorkflow(),
                currentDateTime,
                buildProcesses(processData, currentDateTime)
        );
    }

    private List<ProcessData> getData(final GetBacklogMonitorInputDto input) {
        final Map<ProcessName, List<QuantityByDate>> currentBacklog = getCurrentBacklog(input);
        final Map<ProcessName, List<QuantityByDate>> projectedBacklog = projectedBacklog(input);
        final Map<ProcessName, HistoricalBacklog> historicalBacklog = getHistoricalBacklog(input);

        final GetThroughputResult throughput = getThroughput(input);

        return WORKFLOWS.get(input.getWorkflow())
                .stream()
                .map(p -> new ProcessData(
                        p,
                        currentBacklog.getOrDefault(p, emptyList()),
                        projectedBacklog.getOrDefault(p, emptyList()),
                        historicalBacklog.getOrDefault(p, emptyBacklog()),
                        throughput.getOrDefault(p, emptyMap())
                ))
                .collect(Collectors.toList());
    }

    private Map<ProcessName, List<QuantityByDate>> getCurrentBacklog(
            final GetBacklogMonitorInputDto input) {

        final List<Backlog> backlog = backlogApiGateway.getBacklog(
                BacklogRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflows(of(OUTBOUND_ORDERS))
                        .processes(outboundProcessesNames())
                        .groupingFields(of("process"))
                        .dateFrom(input.getDateFrom())
                        .dateTo(DateUtils.getCurrentUtcDateTime())
                        .build()
        );

        final Map<ProcessName, List<Backlog>> backlogByProcess = backlogByProcess(
                backlog,
                WORKFLOWS.get(OUTBOUND_ORDERS),
                input.getDateFrom()
        );

        return backlogByProcess.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()
                                .stream()
                                .map(b -> new QuantityByDate(b.getDate(), b.getTotal()))
                                .collect(Collectors.toList())
                ));
    }

    private Map<ProcessName, List<Backlog>> backlogByProcess(final List<Backlog> backlog,
                                                             final List<ProcessName> processes,
                                                             final ZonedDateTime dateFrom) {

        final ZonedDateTime currentDatetime = currentDatetime(backlog);
        final List<Backlog> truncatedBacklog = truncateHours(backlog, currentDatetime);

        final Map<ProcessName, List<Backlog>> backlogByProcess = truncatedBacklog.stream()
                .collect(Collectors.groupingBy(
                        this::processNameFromBacklog,
                        Collectors.toList()
                ));

        return processes.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        process ->
                                fillMissing(
                                        backlogByProcess.getOrDefault(process, emptyList()),
                                        dateFrom,
                                        currentDatetime,
                                        date -> new Backlog(date, null, 0)
                                )
                ));
    }

    private Map<ProcessName, HistoricalBacklog> getHistoricalBacklog(
            final GetBacklogMonitorInputDto input) {

        final ZonedDateTime dateFrom = input.getDateFrom().minusWeeks(BACKLOG_WEEKS_LOOKBACK);
        final ZonedDateTime dateTo = input.getDateFrom();

        try {
            return getHistoricalBacklog.execute(
                    GetHistoricalBacklogInput.builder()
                            .warehouseId(input.getWarehouseId())
                            .workflows(of(input.getWorkflow()))
                            .processes(WORKFLOWS.get(input.getWorkflow()))
                            .dateFrom(dateFrom)
                            .dateTo(dateTo)
                            .build()
            );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return emptyMap();
    }

    private Map<ProcessName, List<QuantityByDate>> projectedBacklog(
            final GetBacklogMonitorInputDto input) {

        try {
            return getProjectedBacklog(input);
        } catch (RuntimeException e) {
            log.error("could not retrieve backlog projections", e);
        }
        return emptyMap();
    }

    private Map<ProcessName, List<QuantityByDate>> getProjectedBacklog(
            final GetBacklogMonitorInputDto input) {

        final ZonedDateTime dateFrom = DateUtils.getCurrentUtcDateTime();

        final ZonedDateTime dateTo = input.getDateTo()
                .truncatedTo(ChronoUnit.HOURS)
                .minusHours(1L);

        final List<BacklogProjectionResponse> projectedBacklog = backlogProjection.execute(
                BacklogProjectionInput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .warehouseId(input.getWarehouseId())
                        .processName(WORKFLOWS.get(input.getWorkflow()))
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .groupType("order")
                        .userId(input.getCallerId())
                        .build()
        ).getProjections();

        return projectedBacklog.stream()
                .collect(Collectors.groupingBy(
                        BacklogProjectionResponse::getProcessName,
                        Collectors.flatMapping(p -> p.getValues()
                                        .stream()
                                        .map(v ->
                                                new QuantityByDate(v.getDate(), v.getQuantity())),
                                Collectors.toList()))
                );
    }

    private GetThroughputResult getThroughput(final GetBacklogMonitorInputDto input) {
        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(FBM_WMS_OUTBOUND)
                .processes(WORKFLOWS.get(input.getWorkflow()))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build();

        try {
            return getProcessThroughput.execute(request);
        } catch (RuntimeException e) {
            log.error("could not retrieve throughput for {}", request, e);
        }
        return GetThroughputResult.emptyThroughput();
    }

    private List<ProcessDetail> buildProcesses(final List<ProcessData> data,
                                               final ZonedDateTime currentDateTime) {

        return data.stream()
                .map(detail -> build(
                                detail.getProcess(),
                                currentDateTime,
                                toProcessDescription(detail)
                        )
                ).collect(Collectors.toList());
    }

    private List<BacklogStatsByDate> toProcessDescription(final ProcessData data) {
        final Map<ZonedDateTime, Integer> throughput = data.getThroughputByDate();
        final HistoricalBacklog historical = data.getHistoricalBacklog();

        return Stream.concat(
                toBacklogStatsByDate(data.getCurrentBacklog(), throughput, historical),
                toBacklogStatsByDate(data.getProjectedBacklog(), throughput, historical)
        ).collect(Collectors.toList());
    }

    private Stream<BacklogStatsByDate> toBacklogStatsByDate(
            final List<QuantityByDate> backlog,
            final Map<ZonedDateTime, Integer> throughput,
            final HistoricalBacklog historical) {

        return backlog.stream()
                .map(d -> {
                            ZonedDateTime date = d.getDate().truncatedTo(ChronoUnit.HOURS);
                            return new BacklogStatsByDate(
                                    d.getDate(),
                                    d.getQuantity(),
                                    throughput.get(date),
                                    historical.get(date));
                        }
                );
    }

    private List<String> outboundProcessesNames() {
        return WORKFLOWS.get(OUTBOUND_ORDERS)
                .stream()
                .map(ProcessName::getName)
                .collect(Collectors.toList());
    }

    private ProcessName processNameFromBacklog(final Backlog b) {
        return ProcessName.from(b.getKeys().get("process"));
    }

    private ZonedDateTime getCurrentDatetime(final List<ProcessData> processData) {
        return processData.stream()
                .flatMap(p -> p.getCurrentBacklog()
                        .stream()
                        .map(QuantityByDate::getDate))
                .max(naturalOrder())
                .orElse(DateUtils.getCurrentUtcDateTime());
    }

    @Value
    private static class QuantityByDate {
        ZonedDateTime date;
        Integer quantity;
    }

    @Value
    private static class ProcessData {
        ProcessName process;
        List<QuantityByDate> currentBacklog;
        List<QuantityByDate> projectedBacklog;
        HistoricalBacklog historicalBacklog;
        Map<ZonedDateTime, Integer> throughputByDate;
    }
}
