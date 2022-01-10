package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
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
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
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

    private final BacklogApiAdapter backlogApiAdapter;

    private final PlanningModelGateway planningModelGateway;

    private final GetProcessThroughput getProcessThroughput;

    private final GetHistoricalBacklog getHistoricalBacklog;

    private final GetBacklogLimits getBacklogLimits;

    public GetBacklogMonitorDetailsResponse execute(final GetBacklogMonitorDetailsInput input) {
        final List<VariablesPhoto> backlog = getData(input);

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

    private List<String> areas(final List<VariablesPhoto> backlog) {
        return backlog.stream()
                .map(VariablesPhoto::getUnitsByArea)
                .flatMap(units -> units.keySet().stream())
                .filter(a -> !a.equals(NO_AREA))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Instant getDateOfLatestNonProjectionBacklogPhoto(
            final List<VariablesPhoto> backlog,
            final Instant requestInstant
    ) {
        return backlog.stream()
                .filter(stats -> !stats.isProjection())
                .map(VariablesPhoto::getDate)
                .max(Comparator.naturalOrder())
                .orElse(requestInstant);
    }

    private List<VariablesPhoto> getData(final GetBacklogMonitorDetailsInput input) {
        final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());

        final List<Consolidation> currentBacklog = backlogApiAdapter.getCurrentBacklog(
                input.getRequestDate(),
                input.getWarehouseId(),
                of(input.getWorkflow()),
                of(input.getProcess()),
                of(PROCESS),
                input.getDateFrom(),
                input.getDateTo(),
                input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS),
                input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS));

        final Map<Instant, List<NumberOfUnitsInAnArea>> historicBacklog = getPastBacklog(input, currentBacklog);
        final Map<Instant, List<NumberOfUnitsInAnArea>> projectedBacklog = getProjectedBacklog(input, currentBacklog);
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

    private VariablesPhoto toProcessStats(final boolean isProjection,
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
                        backlog -> backlog.getUnits() >= 0 ? backlog.getUnits() : 0));

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

        return new VariablesPhoto(
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

    private Map<Instant, List<NumberOfUnitsInAnArea>> getPastBacklog(final GetBacklogMonitorDetailsInput input,
                                                                     final List<Consolidation> currentBacklog) {

        final List<Consolidation> fixedConsolidation = fixBacklog(
                input.getRequestDate(),
                currentBacklog,
                input.getDateFrom(),
                date -> new Consolidation(date, Map.of("area", NO_AREA), 0)
        );

        return fixedConsolidation.stream()
                .collect(Collectors.groupingBy(
                        Consolidation::getDate,
                        Collectors.mapping(
                                this::backlogToAreas,
                                Collectors.toList())));
    }

    private List<Consolidation> fixBacklog(final Instant requestDate,
                                           final List<Consolidation> consolidation,
                                           final Instant dateFrom,
                                           final Function<Instant, Consolidation> backlogSupplier) {

        final Instant latestPhotoDate = getDateWhenLatestPhotoWasTaken(consolidation, requestDate);
        final List<Consolidation> truncatedConsolidations =
                truncateToHoursTheTakenOnDatesExceptFor(consolidation, latestPhotoDate);
        return fillMissing(truncatedConsolidations, dateFrom, latestPhotoDate, backlogSupplier);
    }

    private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(final GetBacklogMonitorDetailsInput input,
                                                                          final List<Consolidation> currentBacklog) {

        try {

            final Instant dateFrom = input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

            final Instant dateTo = input.getDateTo()
                    .truncatedTo(ChronoUnit.HOURS)
                    .minus(1L, ChronoUnit.HOURS);

            final List<BacklogProjectionResponse> projectedBacklog = backlogApiAdapter
                    .getProjectedBacklog(
                            input.getWarehouseId(),
                            input.getWorkflow(),
                            of(input.getProcess()),
                            dateFrom.atZone(UTC),
                            dateTo.atZone(UTC),
                            input.getCallerId(),
                            currentBacklog);

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
                                                    projectionValue.getQuantity()))))).orElseGet(Collections::emptyMap);
        } catch (RuntimeException e) {
            log.error("could not retrieve backlog projections", e);
        }
        return emptyMap();
    }

    private Map<Instant, Integer> getTargetBacklog(
            final GetBacklogMonitorDetailsInput input) {

        if (!input.getProcess().hasTargetBacklog()) {
            return Collections.emptyMap();
        }

        final TrajectoriesRequest request = TrajectoriesRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .processName(of(input.getProcess()))
                /* Note that the zone is not necessary but the PlanningModelGateway requires it to
                no avail. */
                .dateFrom(ZonedDateTime.ofInstant(input.getDateFrom(), UTC))
                .dateTo(ZonedDateTime.ofInstant(input.getDateTo(), UTC))
                .source(FORECAST)
                .build();

        return planningModelGateway.getPerformedProcessing(request)
                .stream().collect(
                        Collectors.toMap(
                                entity -> entity.getDate().toInstant(),
                                MagnitudePhoto::getValue));
    }

    private Map<Instant, Integer> getThroughput(final GetBacklogMonitorDetailsInput input) {
        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processes(of(input.getProcess()))
                .dateFrom(input.getDateFrom().atZone(UTC))
                .dateTo(input.getDateTo().atZone(UTC))
                .build();

        try {
            return getProcessThroughput.execute(request)
                    .getOrDefault(input.getProcess(), Map.of())
                    .entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toInstant(), Map.Entry::getValue));
        } catch (RuntimeException e) {
            log.error("could not retrieve throughput for {}", request, e);
        }
        return emptyMap();
    }

    private HistoricalBacklog getHistoricalBacklog(final GetBacklogMonitorDetailsInput input) {
        return getHistoricalBacklog.execute(
                new GetHistoricalBacklogInput(
                        input.getRequestDate(),
                        input.getWarehouseId(),
                        of(input.getWorkflow()),
                        of(input.getProcess()),
                        input.getDateFrom(),
                        input.getDateTo())).get(input.getProcess());
    }

    private Map<Instant, BacklogLimit> getBacklogLimits(
            final GetBacklogMonitorDetailsInput input) {

        try {
            return getBacklogLimits.execute(
                    GetBacklogLimitsInput.builder()
                            .warehouseId(input.getWarehouseId())
                            .workflow(input.getWorkflow())
                            .processes(of(input.getProcess()))
                            .dateFrom(input.getDateFrom())
                            .dateTo(input.getDateTo())
                            .build()).get(input.getProcess());

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return emptyMap();
    }

    private List<DetailedBacklogPhoto> getBacklogDetails(final List<VariablesPhoto> backlog,
                                                         final List<String> areas,
                                                         final Instant currentDatetime) {

        return backlog.stream()
                .map(b -> this.toProcessDetail(b, areas, currentDatetime))
                .sorted(comparing(DetailedBacklogPhoto::getDate))
                .collect(Collectors.toList());
    }

    private ProcessDetail getResumedBacklog(final ProcessName process,
                                            final Instant currentDatetime,
                                            final List<VariablesPhoto> variablesTrajectory) {

        return build(
                process,
                currentDatetime,
                variablesTrajectory.stream()
                        .map(current -> new BacklogStatsByDate(
                                current.getDate(),
                                current.getTotal(),
                                current.getHistorical(),
                                current.getMinLimit(),
                                current.getMaxLimit())).collect(Collectors.toList()));
    }

    private DetailedBacklogPhoto toProcessDetail(final VariablesPhoto variablesPhoto,
                                                 final List<String> processAreas,
                                                 final Instant currentDatetime) {

        final UnitMeasure totalBacklog = variablesPhoto.getTotal();
        final UnitMeasure targetBacklog = variablesPhoto.getTarget();

        final List<AreaBacklogDetail> areas = processAreas.isEmpty()
                ? null
                : toAreas(variablesPhoto, processAreas);

        final Instant date = variablesPhoto.getDate().equals(currentDatetime)
                ? currentDatetime
                : variablesPhoto.getDate().truncatedTo(ChronoUnit.HOURS);

        return new DetailedBacklogPhoto(date, targetBacklog, totalBacklog, areas);
    }

    private List<AreaBacklogDetail> toAreas(final VariablesPhoto variablesPhoto,
                                            final List<String> areas) {

        return areas.stream()
                .map(area -> {
                    Integer units = variablesPhoto.getUnitsByArea().getOrDefault(area, 0);
                    Integer throughput = variablesPhoto.getThroughput();

                    return new AreaBacklogDetail(
                            area,
                            variablesPhoto.isProjection()
                                    ? emptyMeasure()
                                    : UnitMeasure.fromUnits(units, throughput));
                }).collect(Collectors.toList());
    }

    private NumberOfUnitsInAnArea backlogToAreas(final Consolidation consolidation) {
        return new NumberOfUnitsInAnArea(
                consolidation.getKeys().get("area"),
                consolidation.getTotal()
        );
    }

    @Value
    private static class NumberOfUnitsInAnArea {
        String area;
        Integer units;
    }

    /**
     * Remembers the value that some backlog related variables have at some instant.
     */
    @Value
    private static class VariablesPhoto {
        /**
         * Tells if this photo is taken in the future. In that case the variables values comes from
         * forecasts.
         */
        boolean isProjection;
        /**
         * The instant when the photo is taken (pass or future).
         */
        Instant date;
        /**
         * The total backlog.
         */
        UnitMeasure total;
        /**
         * The desired backlog.
         */
        UnitMeasure target;
        /**
         * The minimum value the backlog should not break through at that instant.
         */
        UnitMeasure minLimit;
        /**
         * The maximum value the backlog should not break through at that instant.
         */
        UnitMeasure maxLimit;
        /**
         * The number of units processed per hour at that instant.
         */
        Integer throughput;
        /**
         * The total backlog at the same instant of the previous week.
         */
        UnitMeasure historical;
        /**
         * The total backlog broken down by area at that instant.
         */
        Map<String, Integer> unitsByArea;
    }
}
