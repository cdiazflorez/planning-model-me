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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
@SuppressFBWarnings(
        value = "BX_UNBOXING_IMMEDIATELY_REBOXED",
        justification = "Value is unboxed by map"
)
public class GetBacklogMonitorDetails {
    private static final List<ProcessName> PROCESSES = of(WAVING, PICKING, PACKING);

    private final BacklogApiGateway backlogApiGateway;

    private final ProjectBacklog backlogProjection;

    private final PlanningModelGateway planningModelGateway;

    public GetBacklogMonitorDetailsResponse execute(GetBacklogMonitorDetailsInput input) {
        final ProcessBacklogByDate processBacklogByDate = getData(input);

        return new GetBacklogMonitorDetailsResponse(
                processBacklogByDate.getCurrentDatetime(),
                processBacklogByDate.getProcessData()
                        .map(this::toProcessDetail)
                        .sorted(Comparator.comparing(ProcessBacklogDetail::getDate))
                        .collect(Collectors.toList())
        );
    }

    private ProcessBacklogByDate getData(GetBacklogMonitorDetailsInput input) {
        Map<ZonedDateTime, List<UnitsByArea>> historicBacklog = getPastBacklog(input);
        Map<ZonedDateTime, List<UnitsByArea>> projectedBacklog = getProjectedBacklog(input);
        Map<ZonedDateTime, Integer> targetBacklog = getTargetBacklog(input);
        Map<ZonedDateTime, Integer> throughput = getThroughput(input);

        ZonedDateTime currentDatetime = historicBacklog.keySet()
                .stream()
                .max(Comparator.naturalOrder())
                .orElseGet(DateUtils::getCurrentUtcDateTime);

        return new ProcessBacklogByDate(
                currentDatetime,
                Stream.concat(
                        historicBacklog.entrySet()
                                .stream()
                                .map(entry -> toProcessData(entry, targetBacklog, throughput)),
                        projectedBacklog.entrySet()
                                .stream()
                                .map(entry -> toProcessData(entry, targetBacklog, throughput))
                )
        );
    }

    private ProcessData toProcessData(
            Map.Entry<ZonedDateTime, List<UnitsByArea>> entry,
            Map<ZonedDateTime, Integer> targetBacklog,
            Map<ZonedDateTime, Integer> throughput) {
        return new ProcessData(
                entry.getKey(),
                entry.getValue(),
                targetBacklog.get(entry.getKey()),
                throughput.get(entry.getKey()));
    }

    private Map<ZonedDateTime, List<UnitsByArea>> getPastBacklog(
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

    private Map<ZonedDateTime, List<UnitsByArea>> getProjectedBacklog(
            GetBacklogMonitorDetailsInput input) {
        final ZonedDateTime nextHour = DateUtils.getCurrentUtcDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1L);

        final List<BacklogProjectionResponse> projectedBacklog = backlogProjection.execute(
                BacklogProjectionInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(getProcesses(input.getProcess()))
                        .groupType("order")
                        .dateFrom(nextHour)
                        .dateTo(input.getDateTo())
                        .userId(input.getCallerId())
                        .build()
        ).getProjections();

        return projectedBacklog.get(projectedBacklog.size() - 1)
                .getValues()
                .stream()
                .collect(Collectors.toMap(
                        ProjectionValue::getDate,
                        b -> of(
                                new UnitsByArea("N/A", b.getQuantity())
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
        final List<Entity> entities = planningModelGateway.searchEntities(
                SearchEntitiesRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(of(input.getProcess()))
                        .entityTypes(of(THROUGHPUT))
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .source(SIMULATION)
                        .build()
        ).get(THROUGHPUT);

        return entities.stream()
                .collect(Collectors.toMap(Entity::getDate, Entity::getValue));
    }

    private ProcessBacklogDetail toProcessDetail(ProcessData data) {
        boolean hasAreas = data.getAreas().size() > 1;
        Integer throughput = data.getThroughput();

        Integer totalUnits = data.getAreas()
                .stream()
                .mapToInt(UnitsByArea::getUnits)
                .sum();
        Integer totalMinutes = inMinutes(totalUnits, throughput);

        UnitMeasure targetBacklog = Optional.ofNullable(data.getTargetBacklog())
                .map(t -> new UnitMeasure(t, inMinutes(t, throughput)))
                .orElse(null);

        List<AreaBacklogDetail> areas = hasAreas
                ? toAreas(data.getAreas(), throughput)
                : null;

        UnitMeasure totalBacklog = new UnitMeasure(totalUnits, totalMinutes);

        return new ProcessBacklogDetail(
                data.getDate(),
                targetBacklog,
                totalBacklog,
                areas
        );
    }

    private List<AreaBacklogDetail> toAreas(List<UnitsByArea> areas, Integer throughput) {
        return areas.stream()
                .map(a ->
                        new AreaBacklogDetail(
                                a.getId(),
                                new UnitMeasure(
                                        a.getUnits(),
                                        inMinutes(a.getUnits(), throughput)))
                ).collect(Collectors.toList());
    }

    private Integer inMinutes(Integer units, Integer throughput) {
        if (throughput == null || throughput == 0) {
            return null;
        }

        return (int) Math.ceil((double) units / throughput);
    }

    private UnitsByArea backlogToAreas(Backlog backlog) {
        return new UnitsByArea(
                backlog.getKeys().get("area"),
                backlog.getTotal()
        );
    }

    private List<ProcessName> getProcesses(ProcessName to) {
        return PROCESSES.subList(0, PROCESSES.indexOf(to) + 1);
    }

    @Value
    private static class UnitsByArea {
        String id;
        Integer units;
    }

    @Value
    private static class ProcessData {
        ZonedDateTime date;
        List<UnitsByArea> areas;
        Integer targetBacklog;
        Integer throughput;
    }

    @Value
    private static class ProcessBacklogByDate {
        ZonedDateTime currentDatetime;
        Stream<ProcessData> processData;
    }
}
