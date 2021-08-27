package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.BacklogByDate;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitor {
    private static final long BACKLOG_WEEKS_LOOKBACK = 3L;

    private static final String OUTBOUND_ORDERS = "outbound-orders";

    private static final Map<String, List<String>> WORKFLOWS = Map.of(
            OUTBOUND_ORDERS, of("waving", "picking", "packing")
    );

    private final BacklogApiGateway backlogApiGateway;

    private final PlanningModelGateway planningModelGateway;

    private final ProjectBacklog backlogProjection;

    public WorkflowBacklogDetail execute(GetBacklogMonitorInputDto input) {
        final Map<Process, ProcessData> processData = getData(input);

        return new WorkflowBacklogDetail(
                input.getWorkflow(),
                buildProcesses(input.getWorkflow(), processData)
        );
    }

    private Map<Process, ProcessData> getData(GetBacklogMonitorInputDto input) {
        final Map<Process, List<QuantityByDate>> currentBacklog = getCurrentBacklog(input);
        final Map<Process, List<QuantityByDate>> projectedBacklog = getProjectedBacklog(input);
        final Map<Process, Map<Integer, Integer>> historicalBacklog = getHistoricalBacklog(input);
        final Map<Process, List<QuantityByDate>> productivity = getProductivity(input);

        return WORKFLOWS.entrySet()
                .stream()
                .flatMap(w -> w.getValue()
                        .stream()
                        .map(p -> new Process(w.getKey(), p)))
                .collect(Collectors.toMap(
                        Function.identity(),
                        p -> new ProcessData(
                                currentBacklog.getOrDefault(p, emptyList()),
                                projectedBacklog.getOrDefault(p, emptyList()),
                                historicalBacklog.getOrDefault(p, emptyMap()),
                                productivity.getOrDefault(p, emptyList())
                        )
                ));
    }

    private Map<Process, List<QuantityByDate>> getCurrentBacklog(GetBacklogMonitorInputDto input) {
        return getBacklog(
                input.getWarehouseId(),
                input.getDateFrom(),
                DateUtils.getCurrentUtcDateTime());
    }

    private Map<Process, Map<Integer, Integer>> getHistoricalBacklog(
            GetBacklogMonitorInputDto input) {

        final ZonedDateTime dateFrom = input.getDateFrom().minusWeeks(BACKLOG_WEEKS_LOOKBACK);
        final ZonedDateTime dateTo = input.getDateFrom();

        try {
            return getBacklog(input.getWarehouseId(), dateFrom, dateTo)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> backlogAvgByMinuteInWeek(e.getValue()))
                    );
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return emptyMap();
    }

    private Map<Integer, Integer> backlogAvgByMinuteInWeek(List<QuantityByDate> backlog) {
        return backlog.stream()
                .collect(Collectors.groupingBy(
                                quantity -> minutesFromWeekStart(quantity.getDate()),
                                Collectors.collectingAndThen(
                                        Collectors.averagingInt(QuantityByDate::getQuantity),
                                        Double::intValue
                                )
                        )
                );
    }

    private Map<Process, List<QuantityByDate>> getBacklog(
            String warehouseId,
            ZonedDateTime dateFrom,
            ZonedDateTime dateTo) {

        final List<Backlog> backlog = backlogApiGateway.getBacklog(
                BacklogRequest.builder()
                        .warehouseId(warehouseId)
                        .workflows(of(OUTBOUND_ORDERS))
                        .processes(WORKFLOWS.get(OUTBOUND_ORDERS))
                        .groupingFields(of("workflow", "process", "date_out"))
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .build()
        );

        return backlog.stream()
                .collect(Collectors.groupingBy(
                        this::makeProcessFromBacklog,
                        Collectors.mapping(
                                b -> new QuantityByDate(getDateOut(b), b.getTotal()),
                                Collectors.toList()))
                );
    }

    private Map<Process, List<QuantityByDate>> getProjectedBacklog(
            GetBacklogMonitorInputDto input) {

        final ZonedDateTime nextHour = DateUtils.getCurrentUtcDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1L);

        final List<BacklogProjectionResponse> projectedBacklog = backlogProjection.execute(
                BacklogProjectionInput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .warehouseId(input.getWarehouseId())
                        .processName(outboundProcessesNames())
                        .dateFrom(nextHour)
                        .dateTo(input.getDateTo())
                        .groupType("order")
                        .userId(input.getCallerId())
                        .build()
        ).getProjections();

        return projectedBacklog.stream()
                .collect(Collectors.groupingBy(
                        p -> new Process(OUTBOUND_ORDERS, p.getProcessName().getName()),
                        Collectors.flatMapping(p -> p.getValues()
                                        .stream()
                                        .map(v ->
                                                new QuantityByDate(v.getDate(), v.getQuantity())),
                                Collectors.toList()))
                );
    }

    private Map<Process, List<QuantityByDate>> getProductivity(GetBacklogMonitorInputDto input) {
        final List<Entity> entities = planningModelGateway.searchEntities(
                SearchEntitiesRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .entityTypes(List.of(THROUGHPUT))
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .processName(outboundProcessesNames())
                        .build()
        ).get(THROUGHPUT);

        return entities.stream()
                .collect(Collectors.groupingBy(
                        e -> new Process(OUTBOUND_ORDERS, e.getProcessName().getName()),
                        Collectors.mapping(
                                e -> new QuantityByDate(e.getDate(), e.getValue()),
                                Collectors.toList()))
                );
    }

    private List<ProcessDetail> buildProcesses(String workflow, Map<Process, ProcessData> data) {
        return WORKFLOWS.get(workflow)
                .stream()
                .map(process ->
                        buildProcessDetail(process, data.get(new Process(workflow, process)))
                ).collect(Collectors.toList());
    }

    private ProcessDetail buildProcessDetail(String process, ProcessData data) {
        final Map<Integer, Integer> historicalBacklog = data.getHistoricalBacklog();

        final Map<ZonedDateTime, Integer> productivityByDate = data.getProductivity()
                .stream()
                .collect(Collectors.toMap(
                        QuantityByDate::getDate,
                        QuantityByDate::getQuantity
                ));

        final List<BacklogByDate> backlog =
                toBacklogByDate(data.getCurrentBacklog(), historicalBacklog, productivityByDate);

        final List<BacklogByDate> projections =
                toBacklogByDate(data.getProjectedBacklog(), historicalBacklog, productivityByDate);

        final UnitMeasure totals = backlog.stream()
                .max(comparing(BacklogByDate::getDate))
                .map(BacklogByDate::getCurrent)
                .orElse(new UnitMeasure(0, 0));

        backlog.addAll(projections);

        return new ProcessDetail(process, totals, backlog);
    }

    private List<BacklogByDate> toBacklogByDate(
            List<QuantityByDate> backlog,
            Map<Integer, Integer> historicalBacklog,
            Map<ZonedDateTime, Integer> productivityByDate) {

        return backlog.stream()
                .map(b -> {
                            final ZonedDateTime lookupDate = b.getDate()
                                    .truncatedTo(ChronoUnit.HOURS);

                            final Integer currentProductivity =
                                    productivityByDate.getOrDefault(lookupDate, 1);

                            final Integer historicalQuantity = historicalBacklog.getOrDefault(
                                    minutesFromWeekStart(lookupDate), 0);

                            return BacklogByDate.builder()
                                    .date(b.getDate())
                                    .current(
                                            new UnitMeasure(
                                                    b.getQuantity(),
                                                    inMinutes(b.getQuantity(), currentProductivity))
                                    )
                                    .historical(historicalQuantity)
                                    .build();
                        }
                ).collect(Collectors.toList());
    }

    private List<ProcessName> outboundProcessesNames() {
        return WORKFLOWS.get(OUTBOUND_ORDERS)
                .stream()
                .map(ProcessName::from)
                .collect(Collectors.toList());
    }

    private Integer inMinutes(Integer quantity, Integer productivity) {
        if (productivity == 0) {
            return null;
        }
        return quantity / productivity;
    }

    private Integer minutesFromWeekStart(ZonedDateTime date) {
        return date.getDayOfWeek().getValue() * 1440
                + date.getHour() * 60
                + date.getMinute();
    }

    private Process makeProcessFromBacklog(Backlog b) {
        final Map<String, String> keys = b.getKeys();
        return new Process(keys.get("workflow"), keys.get("process"));
    }

    private ZonedDateTime getDateOut(Backlog backlog) {
        return ZonedDateTime.parse(
                backlog.getKeys().get("date_out"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Value
    private static class Process {
        String workflow;
        String process;
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
        Map<Integer, Integer> historicalBacklog;
        List<QuantityByDate> productivity;
    }
}
