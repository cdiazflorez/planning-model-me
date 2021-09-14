package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Comparator.comparing;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitorDetails {
    private static final List<ProcessName> PROCESSES = of(WAVING, PICKING, PACKING);

    private final BacklogApiGateway backlogApiGateway;

    private final ProjectBacklog backlogProjection;

    private final PlanningModelGateway planningModelGateway;

    private final GetProcessThroughput getProcessThroughput;

    public GetBacklogMonitorDetailsResponse execute(GetBacklogMonitorDetailsInput input) {
        final ProcessBacklog processBacklog = getData(input);

        return new GetBacklogMonitorDetailsResponse(
                processBacklog.getCurrentDatetime(),
                processBacklog.getProcessData()
                        .map(b -> this.toProcessDetail(b, processBacklog.getAreas()))
                        .sorted(comparing(ProcessBacklogDetail::getDate))
                        .collect(Collectors.toList())
        );
    }

    private ProcessBacklog getData(GetBacklogMonitorDetailsInput input) {
        final Map<ZonedDateTime, List<BacklogByArea>> historicBacklog = getPastBacklog(input);
        final Map<ZonedDateTime, List<BacklogByArea>> projectedBacklog = getProjectedBacklog(input);
        final Map<ZonedDateTime, Integer> targetBacklog = getTargetBacklog(input);
        final Map<ZonedDateTime, Integer> throughput = getThroughput(input);

        final ZonedDateTime currentDatetime = historicBacklog.keySet()
                .stream()
                .max(Comparator.naturalOrder())
                .orElseGet(DateUtils::getCurrentUtcDateTime);

        final List<String> areas = historicBacklog.values()
                .stream()
                .flatMap(l -> l.stream().map(BacklogByArea::getArea))
                .filter(a -> !a.equals("N/A"))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return new ProcessBacklog(
                currentDatetime,
                areas,
                Stream.concat(
                        historicBacklog.entrySet()
                                .stream()
                                .map(entry ->
                                        toProcessData(entry, false, targetBacklog, throughput)),
                        projectedBacklog.entrySet()
                                .stream()
                                .map(entry ->
                                        toProcessData(entry, true, targetBacklog, throughput))
                )
        );
    }

    private ProcessStatsByDate toProcessData(
            Map.Entry<ZonedDateTime, List<BacklogByArea>> entry,
            boolean isProjection,
            Map<ZonedDateTime, Integer> targetBacklog,
            Map<ZonedDateTime, Integer> throughput) {
        return new ProcessStatsByDate(
                entry.getKey(),
                isProjection,
                entry.getValue()
                        .stream().collect(Collectors.toMap(
                                BacklogByArea::getArea,
                                BacklogByArea::getUnits
                        )),
                targetBacklog.get(entry.getKey().truncatedTo(ChronoUnit.HOURS)),
                throughput.get(entry.getKey().truncatedTo(ChronoUnit.HOURS)));
    }

    private Map<ZonedDateTime, List<BacklogByArea>> getPastBacklog(
            GetBacklogMonitorDetailsInput input) {

        final List<Backlog> backlog = backlogApiGateway.getBacklog(
                BacklogRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflows(of("outbound-orders"))
                        .processes(of(input.getProcess().getName()))
                        .groupingFields(of("area"))
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build()
        );

        return backlog.stream()
                .collect(Collectors.groupingBy(
                        Backlog::getDate,
                        Collectors.mapping(
                                this::backlogToAreas,
                                Collectors.toList()
                        )
                ));
    }

    private Map<ZonedDateTime, List<BacklogByArea>> getProjectedBacklog(
            GetBacklogMonitorDetailsInput input) {
        final ZonedDateTime dateFrom = DateUtils.getCurrentUtcDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1L);

        final ZonedDateTime dateTo = input.getDateTo()
                .truncatedTo(ChronoUnit.HOURS)
                .minusHours(1L);

        final List<BacklogProjectionResponse> projectedBacklog = backlogProjection.execute(
                BacklogProjectionInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(getProcesses(input.getProcess()))
                        .groupType("order")
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .userId(input.getCallerId())
                        .build()
        ).getProjections();

        return projectedBacklog.get(projectedBacklog.size() - 1)
                .getValues()
                .stream()
                .collect(Collectors.toMap(
                        ProjectionValue::getDate,
                        b -> of(
                                new BacklogByArea("N/A", b.getQuantity())
                        )
                ));
    }

    private Map<ZonedDateTime, Integer> getTargetBacklog(GetBacklogMonitorDetailsInput input) {
        if (!input.getProcess().equals(WAVING)) {
            return Collections.emptyMap();
        }

        EntityRequest request = EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(input.getWarehouseId())
                .processName(of(input.getProcess()))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .source(FORECAST)
                .build();

        return planningModelGateway.getPerformedProcessing(request)
                .stream()
                .collect(Collectors.toMap(Entity::getDate, Entity::getValue));

    }

    private Map<ZonedDateTime, Integer> getThroughput(GetBacklogMonitorDetailsInput input) {
        return getProcessThroughput.execute(GetThroughputInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processes(of(input.getProcess()))
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build())
                .get(input.getProcess());
    }

    private ProcessBacklogDetail toProcessDetail(
            ProcessStatsByDate data,
            List<String> processAreas) {

        final Integer throughput = data.getThroughput();

        final Integer totalUnits = data.getUnitsByArea()
                .values()
                .stream()
                .reduce(0, Integer::sum);
        final Integer totalMinutes = inMinutes(totalUnits, throughput);
        final UnitMeasure totalBacklog = new UnitMeasure(totalUnits, totalMinutes);

        final UnitMeasure targetBacklog = Optional.ofNullable(data.getTargetBacklog())
                .map(t -> new UnitMeasure(t, inMinutes(t, throughput)))
                .orElse(null);

        final boolean hasAreas = !processAreas.isEmpty();
        final List<AreaBacklogDetail> areas = hasAreas ? toAreas(data, processAreas) : null;

        return new ProcessBacklogDetail(
                data.getDate(),
                targetBacklog,
                totalBacklog,
                areas
        );
    }

    private List<AreaBacklogDetail> toAreas(ProcessStatsByDate stats, List<String> areas) {
        return areas.stream()
                .map(area -> toArea(
                        area,
                        stats.isProjection(),
                        stats.getUnitsByArea().getOrDefault(area, 0),
                        stats.getThroughput())
                ).collect(Collectors.toList());
    }

    private AreaBacklogDetail toArea(
            String name,
            boolean isProjection,
            Integer units,
            Integer throughput) {

        return new AreaBacklogDetail(
                name,
                isProjection
                        ? new UnitMeasure(null, null)
                        : new UnitMeasure(units, inMinutes(units, throughput))
        );
    }

    private Integer inMinutes(Integer units, Integer throughput) {
        if (throughput == null || throughput == 0) {
            return null;
        }

        return (int) Math.ceil((double) units / throughput);
    }

    private BacklogByArea backlogToAreas(Backlog backlog) {
        return new BacklogByArea(
                backlog.getKeys().get("area"),
                backlog.getTotal()
        );
    }

    private List<ProcessName> getProcesses(ProcessName to) {
        return PROCESSES.subList(0, PROCESSES.indexOf(to) + 1);
    }

    @Value
    private static class BacklogByArea {
        String area;
        Integer units;
    }

    @Value
    private static class ProcessStatsByDate {
        ZonedDateTime date;
        boolean isProjection;
        Map<String, Integer> unitsByArea;
        Integer targetBacklog;
        Integer throughput;
    }

    @Value
    private static class ProcessBacklog {
        ZonedDateTime currentDatetime;
        List<String> areas;
        Stream<ProcessStatsByDate> processData;
    }
}
