package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.Instant;
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

import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.emptyMeasure;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromMinutes;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitorDetails extends GetConsolidatedBacklog {
    private static final String NO_AREA = "N/A";

    private static final List<ProcessName> PROCESSES = of(WAVING, PICKING, PACKING);

    private final BacklogApiGateway backlogApiGateway;

    private final ProjectBacklog backlogProjection;

    private final PlanningModelGateway planningModelGateway;

    private final GetProcessThroughput getProcessThroughput;

    private final GetHistoricalBacklog getHistoricalBacklog;

    private final GetBacklogLimits getBacklogLimits;

    public GetBacklogMonitorDetailsResponse execute(final GetBacklogMonitorDetailsInput input) {
        final List<BacklogPhotoSummary> backlog = getData(input);

        final Instant currentDatetime =
                getDateOfLatestNonProjectionBacklogPhoto(backlog, input.getRequestDate());
        final List<String> areas = input.getProcess().hasAreas()
                ? areas(backlog)
                : emptyList();

        return new GetBacklogMonitorDetailsResponse(
                currentDatetime,
                getBacklogDetails(backlog, areas, currentDatetime),
                getResumedBacklog(input.getProcess(), currentDatetime, backlog)
        );
    }

    private List<String> areas(final List<BacklogPhotoSummary> backlog) {
        return backlog.stream()
                .map(BacklogPhotoSummary::getUnitsByArea)
                .flatMap(units -> units.keySet().stream())
                .filter(a -> !a.equals(NO_AREA))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Instant getDateOfLatestNonProjectionBacklogPhoto(
            final List<BacklogPhotoSummary> backlog,
            final Instant requestInstant
    ) {
        return backlog.stream()
                .filter(stats -> !stats.isProjection())
                .map(BacklogPhotoSummary::getDate)
                .max(Comparator.naturalOrder())
                .orElse(requestInstant);
    }

    private List<BacklogPhotoSummary> getData(final GetBacklogMonitorDetailsInput input) {
        final Map<Instant, List<NumberOfUnitsInAnArea>> historicBacklog = getPastBacklog(input);
        final Map<Instant, List<NumberOfUnitsInAnArea>> projectedBacklog =
                getProjectedBacklog(input);
        final Map<Instant, BacklogLimit> limits = getBacklogLimits(input);
        final Map<Instant, Integer> targetBacklog = getTargetBacklog(input);
        final Map<Instant, Integer> throughput = getThroughput(input);
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
                                        historicalBacklog,
                                        limits)),
                projectedBacklog.entrySet()
                        .stream()
                        .map(entry ->
                                toProcessStats(
                                        true,
                                        entry.getKey(),
                                        entry.getValue(),
                                        targetBacklog,
                                        throughput,
                                        historicalBacklog,
                                        limits))
        ).collect(Collectors.toList());
    }

    private BacklogPhotoSummary toProcessStats(final boolean isProjection,
                                               final Instant date,
                                               final List<NumberOfUnitsInAnArea> areas,
                                               final Map<Instant, Integer> targetBacklog,
                                               final Map<Instant, Integer> throughput,
                                               final HistoricalBacklog historicalBacklog,
                                               final Map<Instant, BacklogLimit> limits) {

        final Instant truncatedDate = date.truncatedTo(ChronoUnit.HOURS);

        final Map<String, Integer> unitsByArea = areas.stream()
                .collect(Collectors.toMap(
                        NumberOfUnitsInAnArea::getArea,
                        backlog -> backlog.getUnits() >= 0 ? backlog.getUnits() : 0
                ));

        final Integer totalUnits = unitsByArea.values()
                .stream()
                .reduce(0, Integer::sum);

        final Integer throughputValue = throughput.get(truncatedDate);

        final UnitMeasure total = UnitMeasure.fromUnits(totalUnits, throughputValue);
        final UnitMeasure target = Optional.ofNullable(targetBacklog.get(truncatedDate))
                .map(t -> UnitMeasure.fromUnits(t, throughputValue))
                .orElse(null);

        final BacklogLimit limit = limits.get(truncatedDate);
        final UnitMeasure min = limit == null || limit.getMin() < 0
                ? emptyMeasure() : fromMinutes(limit.getMin(), throughputValue);

        final UnitMeasure max = limit == null || limit.getMax() < 0
                ? emptyMeasure() : fromMinutes(limit.getMax(), throughputValue);

        return new BacklogPhotoSummary(
                isProjection,
                date,
                total,
                target,
                min,
                max,
                throughputValue,
                historicalBacklog.getOr(truncatedDate, UnitMeasure::emptyMeasure),
                unitsByArea);
    }

    private Map<Instant, List<NumberOfUnitsInAnArea>> getPastBacklog(
            final GetBacklogMonitorDetailsInput input) {

        final List<Consolidation> consolidations = backlogApiGateway.getBacklog(
                new BacklogRequest(
                        input.getRequestDate(),
                        input.getWarehouseId(),
                        of("outbound-orders"),
                        of(input.getProcess().getName()),
                        of("area"),
                        input.getDateFrom(),
                        input.getDateTo()
                )
        );

        final List<Consolidation> fixedConsolidation = fixBacklog(
                input.getRequestDate(),
                consolidations,
                input.getDateFrom(),
                date -> new Consolidation(date, Map.of("area", NO_AREA), 0)
        );

        return fixedConsolidation.stream()
                .collect(Collectors.groupingBy(
                        Consolidation::getDate,
                        Collectors.mapping(
                                this::backlogToAreas,
                                Collectors.toList()
                        )
                ));
    }

    private List<Consolidation> fixBacklog(final Instant requestInstant,
                                           final List<Consolidation> consolidation,
                                           final Instant dateFrom,
                                           final Function<Instant, Consolidation> backlogSupplier) {

        final Instant currentDatetime =
                getDateWhenLatestPhotoWasTaken(consolidation, requestInstant);
        final List<Consolidation> truncatedConsolidation =
                truncateToHoursTheTakenOnDatesExceptFor(consolidation, currentDatetime);
        return fillMissing(truncatedConsolidation, dateFrom, currentDatetime, backlogSupplier);
    }

    private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(
            final GetBacklogMonitorDetailsInput input) {

        final Instant dateFrom = input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

        final Instant dateTo = input.getDateTo()
                .truncatedTo(ChronoUnit.HOURS)
                .minus(1L, ChronoUnit.HOURS);

        final List<BacklogProjectionResponse> projectedBacklog = backlogProjection.execute(
                BacklogProjectionInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(getProcesses(input.getProcess()))
                        .groupType("order")
                        .dateFrom(dateFrom.atZone(UTC))
                        .dateTo(dateTo.atZone(UTC))
                        .userId(input.getCallerId())
                        .build()
        ).getProjections();

        return projectedBacklog.stream()
                .filter(projection -> projection.getProcessName().equals(input.getProcess()))
                .findFirst()
                .map(projection -> projection.getValues()
                        .stream()
                        .collect(Collectors.toMap(
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

    private Map<Instant, Integer> getTargetBacklog(
            final GetBacklogMonitorDetailsInput input) {

        if (!input.getProcess().hasTargetBacklog()) {
            return Collections.emptyMap();
        }

        final EntityRequest request = EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(input.getWarehouseId())
                .processName(of(input.getProcess()))
                /* Note that the zone is not necessary but the PlanningModelGateway requires it to
                no avail. */
                .dateFrom(ZonedDateTime.ofInstant(input.getDateFrom(), UTC))
                .dateTo(ZonedDateTime.ofInstant(input.getDateTo(), UTC))
                .source(FORECAST)
                .build();

        return planningModelGateway.getPerformedProcessing(request)
                .stream()
                .collect(Collectors.toMap(
                        entity -> entity.getDate().toInstant(),
                        Entity::getValue)
                );

    }

    private Map<Instant, Integer> getThroughput(final GetBacklogMonitorDetailsInput input) {
        return getProcessThroughput.execute(GetThroughputInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processes(of(input.getProcess()))
                        .dateFrom(input.getDateFrom().atZone(UTC))
                        .dateTo(input.getDateTo().atZone(UTC))
                        .build())
                .getOrDefault(input.getProcess(), Map.of())
                .entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toInstant(), Map.Entry::getValue));
    }

    private HistoricalBacklog getHistoricalBacklog(final GetBacklogMonitorDetailsInput input) {
        return getHistoricalBacklog.execute(
                new GetHistoricalBacklogInput(
                        input.getRequestDate(),
                        input.getWarehouseId(),
                        of(input.getWorkflow()),
                        of(input.getProcess()),
                        input.getDateFrom(),
                        input.getDateTo()
                )
        ).get(input.getProcess());
    }

    private Map<Instant, BacklogLimit> getBacklogLimits(
            final GetBacklogMonitorDetailsInput input) {

        try {
            return getBacklogLimits.execute(
                            GetBacklogLimitsInput.builder()
                                    .warehouseId(input.getWarehouseId())
                                    .workflow(FBM_WMS_OUTBOUND)
                                    .processes(of(input.getProcess()))
                                    .dateFrom(input.getDateFrom())
                                    .dateTo(input.getDateTo())
                                    .build()
                    )
                    .get(input.getProcess());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return emptyMap();
    }

    private List<ProcessBacklogDetail> getBacklogDetails(final List<BacklogPhotoSummary> backlog,
                                                         final List<String> areas,
                                                         final Instant currentDatetime) {

        return backlog.stream()
                .map(b -> this.toProcessDetail(b, areas, currentDatetime))
                .sorted(comparing(ProcessBacklogDetail::getDate))
                .collect(Collectors.toList());
    }

    private ProcessDetail getResumedBacklog(final ProcessName process,
                                            final Instant currentDatetime,
                                            final List<BacklogPhotoSummary> backlog) {

        return build(
                process,
                currentDatetime,
                backlog.stream()
                        .map(current -> new BacklogStatsByDate(
                                current.getDate(),
                                current.getTotal(),
                                current.getHistorical(),
                                current.getMinLimit(),
                                current.getMaxLimit()
                        ))
                        .collect(Collectors.toList()));
    }

    private ProcessBacklogDetail toProcessDetail(final BacklogPhotoSummary data,
                                                 final List<String> processAreas,
                                                 final Instant currentDatetime) {

        final UnitMeasure totalBacklog = data.getTotal();
        final UnitMeasure targetBacklog = data.getTarget();

        final List<AreaBacklogDetail> areas = processAreas.isEmpty()
                ? null
                : toAreas(data, processAreas);

        final Instant date = data.getDate().equals(currentDatetime)
                ? currentDatetime
                : data.getDate().truncatedTo(ChronoUnit.HOURS);

        return new ProcessBacklogDetail(date, targetBacklog, totalBacklog, areas);
    }

    private List<AreaBacklogDetail> toAreas(final BacklogPhotoSummary stats,
                                            final List<String> areas) {

        return areas.stream()
                .map(area -> {
                            Integer units = stats.getUnitsByArea().getOrDefault(area, 0);
                            Integer throughput = stats.getThroughput();

                            return new AreaBacklogDetail(
                                    area,
                                    stats.isProjection()
                                            ? emptyMeasure()
                                            : UnitMeasure.fromUnits(units, throughput)
                            );
                        }
                ).collect(Collectors.toList());
    }

    private NumberOfUnitsInAnArea backlogToAreas(final Consolidation consolidation) {
        return new NumberOfUnitsInAnArea(
                consolidation.getKeys().get("area"),
                consolidation.getTotal()
        );
    }

    private List<ProcessName> getProcesses(final ProcessName to) {
        return PROCESSES.subList(0, PROCESSES.indexOf(to) + 1);
    }

    @Value
    private static class NumberOfUnitsInAnArea {
        String area;
        Integer units;
    }

    @Value
    private static class BacklogPhotoSummary {
        boolean isProjection;
        Instant date;
        UnitMeasure total;
        UnitMeasure target;
        UnitMeasure minLimit;
        UnitMeasure maxLimit;
        Integer throughput;
        UnitMeasure historical;
        Map<String, Integer> unitsByArea;
    }
}
