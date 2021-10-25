package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

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
        final ZonedDateTime dateFrom = input.getDateFrom()
                .minusWeeks(BACKLOG_WEEKS_DATE_FROM_LOOKBACK);

        final ZonedDateTime dateTo = input.getDateTo()
                .minusWeeks(1L)
                .plusHours(1L);

        final Map<ProcessName, Map<ZonedDateTime, Integer>> backlogByProcess =
                getBacklogByProcess(input, dateFrom, dateTo);

        final GetThroughputResult throughput = getThroughput(input, dateFrom, dateTo);

        return input.getProcesses()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        process -> toHistoricalBacklog(
                                backlogByProcess.getOrDefault(process, emptyMap()),
                                throughput.getOrDefault(process, emptyMap())
                        ))
                );
    }

    private Map<ProcessName, Map<ZonedDateTime, Integer>> getBacklogByProcess(
            final GetHistoricalBacklogInput input,
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo) {

        final List<Backlog> backlogs = backlogApiGateway.getBacklog(
                BacklogRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflows(input.getWorkflows())
                        .processes(processes(input.getProcesses()))
                        .groupingFields(of("process"))
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .build()
        );

        Predicate<Backlog> filterBy = getBacklogFilter(input.getDateFrom(), input.getDateTo());

        return backlogs.stream()
                .filter(filterBy)
                .collect(Collectors.groupingBy(
                        this::processNameFromBacklog,
                        Collectors.toMap(
                                b -> b.getDate().truncatedTo(ChronoUnit.HOURS),
                                Backlog::getTotal,
                                (v1, v2) -> v1
                        )
                ));
    }

    private GetThroughputResult getThroughput(
            final GetHistoricalBacklogInput input,
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo) {

        return getProcessThroughput.execute(
                GetThroughputInput.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .processes(input.getProcesses())
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .build()
        );
    }

    private HistoricalBacklog toHistoricalBacklog(final Map<ZonedDateTime, Integer> backlog,
                                                  final Map<ZonedDateTime, Integer> throughput) {

        final var measurements = toUnitMeasure(backlog, throughput);
        return new HistoricalBacklog(averageBacklogByDate(measurements));
    }

    private Map<ZonedDateTime, UnitMeasure> toUnitMeasure(
            final Map<ZonedDateTime, Integer> backlog,
            final Map<ZonedDateTime, Integer> throughput) {

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
            final Map<ZonedDateTime, UnitMeasure> backlogs) {

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

    private Predicate<Backlog> getBacklogFilter(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        Integer dateFromInMinutes = minutesFromWeekStart(dateFrom);
        Integer dateToInMinutes = minutesFromWeekStart(dateTo);

        Predicate<Backlog> filterByAny = backlog -> {
            Integer backlogDateInMinutes = minutesFromWeekStart(
                    backlog.getDate()
                            .truncatedTo(ChronoUnit.HOURS)
            );

            return backlogDateInMinutes.compareTo(dateFromInMinutes) >= 0
                    && backlogDateInMinutes.compareTo(dateToInMinutes) <= 0;
        };

        Predicate<Backlog> filterByForEndOfWeek = backlog -> {
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

    private ProcessName processNameFromBacklog(final Backlog b) {
        final Map<String, String> keys = b.getKeys();
        return ProcessName.from(keys.get("process"));
    }
}
