package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
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
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitorDetails extends GetConsolidatedBacklog {
    private static final long BACKLOG_WEEKS_LOOKBACK = 3L;

    private static final String NO_AREA = "N/A";

    private static final List<ProcessName> PROCESSES = of(WAVING, PICKING, PACKING);

    private final BacklogApiGateway backlogApiGateway;

    private final ProjectBacklog backlogProjection;

    private final PlanningModelGateway planningModelGateway;

    private final GetProcessThroughput getProcessThroughput;

    private final GetHistoricalBacklog getHistoricalBacklog;

    public GetBacklogMonitorDetailsResponse execute(final GetBacklogMonitorDetailsInput input) {
        final List<ProcessStatsByDate> backlog = getData(input);

        final ZonedDateTime currentDatetime = getCurrentDatetime(backlog);
        final List<String> areas = input.getProcess().hasAreas()
                ? areas(backlog)
                : emptyList();

        return new GetBacklogMonitorDetailsResponse(
                currentDatetime,
                getBacklogDetails(backlog, areas, currentDatetime),
                getResumedBacklog(input.getProcess(), currentDatetime, backlog)
        );
    }

    private List<String> areas(final List<ProcessStatsByDate> backlog) {
        return backlog.stream()
                .map(ProcessStatsByDate::getUnitsByArea)
                .flatMap(units -> units.keySet().stream())
                .filter(a -> !a.equals(NO_AREA))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private ZonedDateTime getCurrentDatetime(final List<ProcessStatsByDate> backlog) {
        return backlog.stream()
                .filter(stats -> !stats.isProjection())
                .map(ProcessStatsByDate::getDate)
                .max(Comparator.naturalOrder())
                .orElseGet(DateUtils::getCurrentUtcDateTime);
    }

    private List<ProcessStatsByDate> getData(final GetBacklogMonitorDetailsInput input) {
        final Map<ZonedDateTime, List<BacklogByArea>> historicBacklog = getPastBacklog(input);
        final Map<ZonedDateTime, List<BacklogByArea>> projectedBacklog = getProjectedBacklog(input);
        final Map<ZonedDateTime, Integer> targetBacklog = getTargetBacklog(input);
        final Map<ZonedDateTime, Integer> throughput = getThroughput(input);
        final HistoricalBacklog historicalBacklog = getHistoricalBacklog(input);

        return Stream.concat(
                historicBacklog.entrySet()
                        .stream()
                        .map(entry ->
                                toProcessStats(
                                        false,
                                        entry.getKey(),
                                        entry.getValue(),
                                        targetBacklog,
                                        throughput,
                                        historicalBacklog)),
                projectedBacklog.entrySet()
                        .stream()
                        .map(entry ->
                                toProcessStats(
                                        true,
                                        entry.getKey(),
                                        entry.getValue(),
                                        targetBacklog,
                                        throughput,
                                        historicalBacklog))
        ).collect(Collectors.toList());
    }

    private ProcessStatsByDate toProcessStats(final boolean isProjection,
                                              final ZonedDateTime date,
                                              final List<BacklogByArea> areas,
                                              final Map<ZonedDateTime, Integer> targetBacklog,
                                              final Map<ZonedDateTime, Integer> throughput,
                                              final HistoricalBacklog historicalBacklog) {

        final ZonedDateTime truncatedDate = date.truncatedTo(ChronoUnit.HOURS);

        final Map<String, Integer> unitsByArea = areas.stream()
                .collect(Collectors.toMap(
                        BacklogByArea::getArea,
                        backlog -> backlog.getUnits() >= 0 ? backlog.getUnits() : 0
                ));

        final Integer totalUnits = unitsByArea.values()
                .stream()
                .reduce(0, Integer::sum);

        return new ProcessStatsByDate(
                isProjection,
                date,
                totalUnits,
                targetBacklog.get(truncatedDate),
                throughput.get(truncatedDate),
                historicalBacklog.get(truncatedDate),
                unitsByArea);
    }

    private Map<ZonedDateTime, List<BacklogByArea>> getPastBacklog(
            final GetBacklogMonitorDetailsInput input) {

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

        final List<Backlog> fixedBacklog = fixBacklog(
                backlog,
                input.getDateFrom(),
                date -> new Backlog(date, Map.of("area", NO_AREA), 0)
        );

        return fixedBacklog.stream()
                .collect(Collectors.groupingBy(
                        Backlog::getDate,
                        Collectors.mapping(
                                this::backlogToAreas,
                                Collectors.toList()
                        )
                ));
    }

    private List<Backlog> fixBacklog(final List<Backlog> backlog,
                                     final ZonedDateTime dateFrom,
                                     final Function<ZonedDateTime, Backlog> backlogSupplier) {

        final ZonedDateTime currentDatetime = currentDatetime(backlog);
        final List<Backlog> truncatedBacklog = truncateHours(backlog, currentDatetime);
        return fillMissing(truncatedBacklog, dateFrom, currentDatetime, backlogSupplier);
    }

    private Map<ZonedDateTime, List<BacklogByArea>> getProjectedBacklog(
            final GetBacklogMonitorDetailsInput input) {

        final ZonedDateTime dateFrom = DateUtils.getCurrentUtcDateTime()
                .truncatedTo(ChronoUnit.HOURS);

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

        return projectedBacklog.stream()
                .filter(projection -> projection.getProcessName().equals(input.getProcess()))
                .findFirst()
                .map(projection -> projection.getValues()
                        .stream()
                        .collect(Collectors.toMap(
                                ProjectionValue::getDate,
                                b -> of(
                                        new BacklogByArea(NO_AREA, b.getQuantity())
                                )
                        ))
                )
                .orElseGet(Collections::emptyMap);
    }

    private Map<ZonedDateTime, Integer> getTargetBacklog(
            final GetBacklogMonitorDetailsInput input) {

        if (!input.getProcess().hasTargetBacklog()) {
            return Collections.emptyMap();
        }

        final EntityRequest request = EntityRequest.builder()
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

    private Map<ZonedDateTime, Integer> getThroughput(final GetBacklogMonitorDetailsInput input) {
        return getProcessThroughput.execute(GetThroughputInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processes(of(input.getProcess()))
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build())
                .get(input.getProcess());
    }

    private HistoricalBacklog getHistoricalBacklog(final GetBacklogMonitorDetailsInput input) {
        final ZonedDateTime dateFrom = input.getDateFrom().minusWeeks(BACKLOG_WEEKS_LOOKBACK);
        final ZonedDateTime dateTo = input.getDateFrom();

        return getHistoricalBacklog.execute(
                GetHistoricalBacklogInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflows(of(input.getWorkflow()))
                        .processes(of(input.getProcess()))
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .build()
        ).get(input.getProcess());
    }

    private List<ProcessBacklogDetail> getBacklogDetails(final List<ProcessStatsByDate> backlog,
                                                         final List<String> areas,
                                                         final ZonedDateTime currentDatetime) {

        return backlog.stream()
                .map(b -> this.toProcessDetail(b, areas, currentDatetime))
                .sorted(comparing(ProcessBacklogDetail::getDate))
                .collect(Collectors.toList());
    }

    private ProcessDetail getResumedBacklog(final ProcessName process,
                                            final ZonedDateTime currentDatetime,
                                            final List<ProcessStatsByDate> backlog) {

        return build(
                process,
                currentDatetime,
                backlog.stream()
                        .map(current ->
                                new BacklogStatsByDate(
                                        current.getDate(),
                                        current.getTotalUnits(),
                                        current.getThroughput(),
                                        current.getHistorical()
                                ))
                        .collect(Collectors.toList()));
    }

    private ProcessBacklogDetail toProcessDetail(final ProcessStatsByDate data,
                                                 final List<String> processAreas,
                                                 final ZonedDateTime currentDatetime) {

        final Integer throughput = data.getThroughput();
        final Integer totalMinutes = inMinutes(data.getTotalUnits(), throughput);
        final UnitMeasure totalBacklog = new UnitMeasure(data.getTotalUnits(), totalMinutes);

        final UnitMeasure targetBacklog = Optional.ofNullable(data.getTargetBacklog())
                .map(t -> new UnitMeasure(t, inMinutes(t, throughput)))
                .orElse(null);

        final List<AreaBacklogDetail> areas = processAreas.isEmpty()
                ? null
                : toAreas(data, processAreas);

        final ZonedDateTime date = data.getDate().equals(currentDatetime)
                ? currentDatetime
                : data.getDate().truncatedTo(ChronoUnit.HOURS);

        return new ProcessBacklogDetail(date, targetBacklog, totalBacklog, areas);
    }

    private List<AreaBacklogDetail> toAreas(final ProcessStatsByDate stats,
                                            final List<String> areas) {

        return areas.stream()
                .map(area -> {
                            Integer units = stats.getUnitsByArea().getOrDefault(area, 0);
                            Integer throughput = stats.getThroughput();

                            return new AreaBacklogDetail(
                                    area,
                                    stats.isProjection()
                                            ? new UnitMeasure(null, null)
                                            : new UnitMeasure(units, inMinutes(units, throughput))
                            );
                        }
                ).collect(Collectors.toList());
    }

    private BacklogByArea backlogToAreas(final Backlog backlog) {
        return new BacklogByArea(
                backlog.getKeys().get("area"),
                backlog.getTotal()
        );
    }

    private List<ProcessName> getProcesses(final ProcessName to) {
        return PROCESSES.subList(0, PROCESSES.indexOf(to) + 1);
    }

    @Value
    private static class BacklogByArea {
        String area;
        Integer units;
    }

    @Value
    private static class ProcessStatsByDate {
        boolean isProjection;
        ZonedDateTime date;
        Integer totalUnits;
        Integer targetBacklog;
        Integer throughput;
        Integer historical;
        Map<String, Integer> unitsByArea;
    }
}
