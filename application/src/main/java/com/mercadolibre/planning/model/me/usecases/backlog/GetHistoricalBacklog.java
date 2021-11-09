package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.minutesFromWeekStart;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
class GetHistoricalBacklog {
    private static final long BACKLOG_WEEKS_DATE_FROM_LOOKBACK = 2L;

    private final BacklogApiGateway backlogApiGateway;

    private final GetProcessThroughput getProcessThroughput;

    Map<ProcessName, HistoricalBacklog> execute(final GetHistoricalBacklogInput input) {
        final Instant dateFrom = LocalDateTime.ofInstant(input.getDateFrom(), UTC)
                .minusWeeks(BACKLOG_WEEKS_DATE_FROM_LOOKBACK).toInstant(UTC);

        final Instant dateTo = LocalDateTime.ofInstant(input.getDateTo(), UTC)
                .minusWeeks(1L)
                .plusHours(1L)
                .toInstant(UTC);

        final Map<ProcessName, Map<Instant, Integer>> backlogByProcess =
                getBacklogByProcess(input, dateFrom, dateTo);

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
                                                Map.Entry::getValue
                                        ))
                        ))
                );
    }

    private Map<ProcessName, Map<Instant, Integer>> getBacklogByProcess(
            final GetHistoricalBacklogInput input,
            final Instant dateFrom,
            final Instant dateTo) {

        final List<Consolidation> consolidations = backlogApiGateway.getBacklog(
                new BacklogRequest(
                        input.getRequestDate(),
                        input.getWarehouseId(),
                        input.getWorkflows(),
                        processes(input.getProcesses()),
                        of("process"),
                        dateFrom,
                        dateTo
                )
        );

        Predicate<Consolidation> filterBy =
                getBacklogFilter(input.getDateFrom(), input.getDateTo());

        return consolidations.stream()
                .filter(filterBy)
                .collect(Collectors.groupingBy(
                        this::processNameFromBacklog,
                        Collectors.toMap(
                                b -> b.getDate().truncatedTo(ChronoUnit.HOURS),
                                Consolidation::getTotal,
                                (v1, v2) -> v1
                        )
                ));
    }

    private GetThroughputResult getThroughput(
            final GetHistoricalBacklogInput input,
            final Instant dateFrom,
            final Instant dateTo) {

        return getProcessThroughput.execute(
                GetThroughputInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processes(input.getProcesses())
                        .dateFrom(ZonedDateTime.ofInstant(dateFrom, UTC))
                        .dateTo(ZonedDateTime.ofInstant(dateTo, UTC))
                        .build()
        );
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
                                throughput.get(entry.getKey())
                        ))
                );
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
                                        this::average
                                )
                        ))
                );
    }

    private UnitMeasure average(List<UnitMeasure> measures) {
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

    private Predicate<Consolidation> getBacklogFilter(Instant dateFrom, Instant dateTo) {
        Integer dateFromInMinutes = minutesFromWeekStart(dateFrom);
        Integer dateToInMinutes = minutesFromWeekStart(dateTo);

        Predicate<Consolidation> filterByAny = backlog -> {
            Integer backlogDateInMinutes = minutesFromWeekStart(
                    backlog.getDate()
                            .truncatedTo(ChronoUnit.HOURS)
            );

            return backlogDateInMinutes.compareTo(dateFromInMinutes) >= 0
                    && backlogDateInMinutes.compareTo(dateToInMinutes) <= 0;
        };

        Predicate<Consolidation> filterByForEndOfWeek = backlog -> {
            Integer backlogDateInMinutes = minutesFromWeekStart(
                    backlog.getDate()
                            .truncatedTo(ChronoUnit.HOURS)
            );

            return backlogDateInMinutes.compareTo(dateFromInMinutes) >= 0
                    && backlogDateInMinutes.compareTo(dateToInMinutes) <= 0;
        };

        return dateFromInMinutes.compareTo(dateToInMinutes) < 0
                ? filterByAny
                : filterByForEndOfWeek;
    }

    private List<String> processes(final List<ProcessName> processNames) {
        return processNames.stream()
                .map(ProcessName::getName)
                .collect(Collectors.toList());
    }

    private ProcessName processNameFromBacklog(final Consolidation b) {
        final Map<String, String> keys = b.getKeys();
        return ProcessName.from(keys.get("process"));
    }
}
