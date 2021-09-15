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
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.ProcessDetailBuilderInput;
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

    private final ProcessDetailBuilder processDetailBuilder;

    public WorkflowBacklogDetail execute(GetBacklogMonitorInputDto input) {
        final List<ProcessData> processData = getData(input);
        final ZonedDateTime currentDateTime = getCurrentDatetime(processData);

        return new WorkflowBacklogDetail(
                input.getWorkflow(),
                currentDateTime,
                buildProcesses(processData, currentDateTime)
        );
    }

    private List<ProcessData> getData(GetBacklogMonitorInputDto input) {
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
            List<ProcessData> data,
            ZonedDateTime currentDateTime) {

        return data.stream()
                .map(detail -> processDetailBuilder.execute(
                        new ProcessDetailBuilderInput(
                                detail.getProcess(),
                                currentDateTime,
                                toProcessDescription(detail)
                        ))
                ).collect(Collectors.toList());
    }

    private List<BacklogStatsByDate> toProcessDescription(ProcessData data) {
        return Stream.concat(
                data.getCurrentBacklog()
                        .stream()
                        .map(d -> new BacklogStatsByDate(
                                d.getDate(),
                                d.getQuantity(),
                                data.getThroughputByDate().getOrDefault(d.getDate(), 0),
                                data.getHistoricalBacklog().get(d.getDate())
                        )),
                data.getProjectedBacklog()
                        .stream()
                        .map(d -> new BacklogStatsByDate(
                                d.getDate(),
                                d.getQuantity(),
                                data.getThroughputByDate().getOrDefault(d.getDate(), 0),
                                data.getHistoricalBacklog().get(d.getDate())
                        ))
        ).collect(Collectors.toList());
    }

    private List<String> outboundProcessesNames() {
        return WORKFLOWS.get(OUTBOUND_ORDERS)
                .stream()
                .map(ProcessName::getName)
                .collect(Collectors.toList());
    }

    private ProcessName processNameFromBacklog(Backlog b) {
        return ProcessName.from(b.getKeys().get("process"));
    }

    private ZonedDateTime getCurrentDatetime(List<ProcessData> processData) {
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
