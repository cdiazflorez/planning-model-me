package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogsByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
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
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitor {
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
        final Map<ProcessName, ProcessData> processData = getData(input);

        return new WorkflowBacklogDetail(
                input.getWorkflow(),
                getCurrentDatetime(processData),
                buildProcesses(input.getWorkflow(), processData)
        );
    }

    private Map<ProcessName, ProcessData> getData(GetBacklogMonitorInputDto input) {
        final Map<ProcessName, List<QuantityByDate>> currentBacklog = getCurrentBacklog(input);
        final Map<ProcessName, List<QuantityByDate>> projectedBacklog = projectedBacklog(input);
        final Map<ProcessName, HistoricalBacklog> historicalBacklog = getHistoricalBacklog(input);

        final GetThroughputResult throughput = getThroughput(input);

        return WORKFLOWS.get(input.getWorkflow())
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        p -> new ProcessData(
                                currentBacklog.getOrDefault(p, emptyList()),
                                projectedBacklog.getOrDefault(p, emptyList()),
                                historicalBacklog.getOrDefault(p, emptyBacklog()),
                                throughput.getOrDefault(p, emptyMap())
                        )
                ));
    }

    private Map<ProcessName, List<QuantityByDate>> getCurrentBacklog(
            GetBacklogMonitorInputDto input) {

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

        return backlog.stream()
                .collect(Collectors.groupingBy(
                        this::processNameFromBacklog,
                        Collectors.mapping(
                                b -> new QuantityByDate(b.getDate(), b.getTotal()),
                                Collectors.toList()))
                );
    }

    private Map<ProcessName, HistoricalBacklog> getHistoricalBacklog(
            GetBacklogMonitorInputDto input) {

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
            GetBacklogMonitorInputDto input) {

        try {
            return getProjectedBacklog(input);
        } catch (RuntimeException e) {
            log.error("could not retrieve backlog projections", e);
        }
        return emptyMap();
    }

    private Map<ProcessName, List<QuantityByDate>> getProjectedBacklog(
            GetBacklogMonitorInputDto input) {

        final ZonedDateTime dateFrom = DateUtils.getCurrentUtcDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1L);

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

    private GetThroughputResult getThroughput(GetBacklogMonitorInputDto input) {
        GetThroughputInput request = GetThroughputInput.builder()
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

    private List<ProcessDetail> buildProcesses(
            String workflow,
            Map<ProcessName, ProcessData> data) {

        return WORKFLOWS.get(workflow)
                .stream()
                .map(process ->
                        buildProcessDetail(process.getName(), data.get(process))
                ).collect(Collectors.toList());
    }

    private ProcessDetail buildProcessDetail(String process, ProcessData data) {
        final List<BacklogsByDate> backlog = toBacklogByDate(
                data.getCurrentBacklog(),
                data.getHistoricalBacklog(),
                data.getThroughputByDate());

        final List<BacklogsByDate> projections = toBacklogByDate(
                data.getProjectedBacklog(),
                data.getHistoricalBacklog(),
                data.getThroughputByDate());

        final UnitMeasure totals = backlog.stream()
                .max(comparing(BacklogsByDate::getDate))
                .map(BacklogsByDate::getCurrent)
                .map(u ->
                        new UnitMeasure(
                                u.getUnits(),
                                u.getMinutes() == null ? Integer.valueOf(0) : u.getMinutes())
                )
                .orElse(new UnitMeasure(0, 0));

        final List<BacklogsByDate> allBacklog = Stream.concat(
                        backlog.stream(),
                        projections.stream())
                .sorted(comparing(BacklogsByDate::getDate))
                .collect(Collectors.toList());

        return new ProcessDetail(process, totals, allBacklog);
    }

    private List<BacklogsByDate> toBacklogByDate(
            List<QuantityByDate> backlog,
            HistoricalBacklog historicalBacklog,
            Map<ZonedDateTime, Integer> throughputByDate) {

        return backlog.stream()
                .map(b -> {
                            final ZonedDateTime lookupDate = b.getDate()
                                    .truncatedTo(ChronoUnit.HOURS);

                            final Integer currentThroughput =
                                    throughputByDate.getOrDefault(lookupDate, 0);

                            final Integer historicalQuantity =
                                    historicalBacklog.getOr(lookupDate, 0);

                            return BacklogsByDate.builder()
                                    .date(b.getDate())
                                    .current(
                                            new UnitMeasure(
                                                    b.getQuantity(),
                                                    inMinutes(b.getQuantity(), currentThroughput))
                                    )
                                    .historical(
                                            new UnitMeasure(historicalQuantity, null)
                                    )
                                    .build();
                        }
                ).collect(Collectors.toList());
    }

    private List<String> outboundProcessesNames() {
        return WORKFLOWS.get(OUTBOUND_ORDERS)
                .stream()
                .map(ProcessName::getName)
                .collect(Collectors.toList());
    }

    private Integer inMinutes(Integer quantity, Integer throughput) {
        if (throughput == 0) {
            return null;
        }
        return quantity / throughput;
    }

    private ProcessName processNameFromBacklog(Backlog b) {
        final Map<String, String> keys = b.getKeys();
        return ProcessName.from(keys.get("process"));
    }

    private ZonedDateTime getCurrentDatetime(Map<ProcessName, ProcessData> processData) {
        return processData.values()
                .stream()
                .flatMap(
                        p -> p.getCurrentBacklog()
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
        List<QuantityByDate> currentBacklog;
        List<QuantityByDate> projectedBacklog;
        HistoricalBacklog historicalBacklog;
        Map<ZonedDateTime, Integer> throughputByDate;
    }
}
