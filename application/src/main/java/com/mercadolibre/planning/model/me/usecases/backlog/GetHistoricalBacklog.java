package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
import static com.mercadolibre.planning.model.me.utils.DateUtils.minutesFromWeekStart;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
class GetHistoricalBacklog {
    private static final String PROCESS_KEY = "process";
    private static final int BACKLOG_WEEKS_DATE_FROM_LOOKBACK = 3;

    private final BacklogApiAdapter backlogApiAdapter;

    private final GetProcessThroughput getProcessThroughput;

    Map<ProcessName, HistoricalBacklog> execute(final GetHistoricalBacklogInput input) {
        final Instant dateFrom = LocalDateTime.ofInstant(input.getDateFrom(), UTC)
                .minusWeeks(BACKLOG_WEEKS_DATE_FROM_LOOKBACK).toInstant(UTC);

        final Instant dateTo = LocalDateTime.ofInstant(input.getDateTo(), UTC)
                .minusWeeks(1L)
                .plusHours(1L)
                .toInstant(UTC);

        final Map<ProcessName, Map<Instant, Integer>> backlogByProcess = getBacklogByProcess(input);

        final GetThroughputResult throughput = getThroughput(input, dateFrom, dateTo);

        return input.getProcesses()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        process -> toHistoricalBacklog(
                                backlogByProcess.getOrDefault(process, emptyMap()),
                                throughput.getOrDefault(process, emptyMap())
                                        .entrySet().stream()
                                        .collect(Collectors.toMap(
                                                e -> e.getKey().toInstant(),
                                                Map.Entry::getValue)))));
    }

    private Map<ProcessName, Map<Instant, Integer>> getBacklogByProcess(final GetHistoricalBacklogInput input) {
        final Stream<Consolidation> consolidations = IntStream.rangeClosed(1, BACKLOG_WEEKS_DATE_FROM_LOOKBACK)
                .mapToObj(shift -> getConsolidatedTrajectory(input, Duration.ofDays(7L * shift)).stream())
                .flatMap(Function.identity());

        return consolidations
                .collect(Collectors.groupingBy(
                        this::processNameFromBacklog,
                        Collectors.toMap(
                                b -> b.getDate().truncatedTo(HOURS),
                                Consolidation::getTotal,
                                Integer::sum)));
    }

    private List<Consolidation> getConsolidatedTrajectory(final GetHistoricalBacklogInput input,
                                                          final Duration shift) {

        final Instant adjustedRequestDate = input.getRequestDate().minus(shift);
        final Instant adjustedDateFrom = input.getDateFrom().minus(shift);
        final Instant adjustedDateTo = input.getDateTo().minus(shift)
                .plus(5, MINUTES); // adds five minutes to take into account the consolidations job's delays

        final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());
        final int offsetFrom = workflow.getSlaFromOffsetInHours();
        final int offsetTo = workflow.getSlaToOffsetInHours();

        return backlogApiAdapter.getCurrentBacklog(
                input.getRequestDate(),
                input.getWarehouseId(),
                of(input.getWorkflow()),
                input.getProcesses(),
                of(PROCESS),
                adjustedDateFrom,
                adjustedDateTo,
                adjustedRequestDate.minus(offsetFrom, HOURS),
                adjustedRequestDate.plus(offsetTo, HOURS));
    }

    private GetThroughputResult getThroughput(
            final GetHistoricalBacklogInput input,
            final Instant dateFrom,
            final Instant dateTo) {

        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processes(input.getProcesses())
                /* Note that the zone is not necessary but the GetProcessThroughput use case
                 requires it to no avail. */
                .dateFrom(ZonedDateTime.ofInstant(dateFrom, UTC))
                .dateTo(ZonedDateTime.ofInstant(dateTo, UTC))
                .build();

        try {
            return getProcessThroughput.execute(request);
        } catch (RuntimeException e) {
            log.error("could not retrieve throughput for {}", request, e);
            return GetThroughputResult.emptyThroughput();
        }
    }

    private HistoricalBacklog toHistoricalBacklog(final Map<Instant, Integer> backlog,
                                                  final Map<Instant, Integer> throughput) {

        final var measurements = toUnitMeasure(backlog, throughput);
        return new HistoricalBacklog(averageBacklogByDate(measurements));
    }

    private Map<Instant, UnitMeasure> toUnitMeasure(
            final Map<Instant, Integer> backlog,
            final Map<Instant, Integer> throughput) {

        return backlog.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> UnitMeasure.fromUnits(
                                entry.getValue(),
                                throughput.get(entry.getKey()))));
    }

    private Map<Integer, UnitMeasure> averageBacklogByDate(
            final Map<Instant, UnitMeasure> backlogs) {

        return backlogs.entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        entry -> minutesFromWeekStart(entry.getKey()),
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        this::average))));
    }

    private UnitMeasure average(final List<UnitMeasure> measures) {
        final double units = measures.stream()
                .mapToInt(UnitMeasure::getUnits)
                .average()
                .orElse(0.0);

        final double minutes = measures.stream()
                .map(UnitMeasure::getMinutes)
                .filter(Objects::nonNull)
                .mapToInt(mins -> mins)
                .average()
                .orElse(0.0);

        return new UnitMeasure((int) units, (int) minutes);
    }

    private ProcessName processNameFromBacklog(final Consolidation b) {
        final Map<String, String> keys = b.getKeys();
        return ProcessName.from(keys.get(PROCESS_KEY));
    }
}
