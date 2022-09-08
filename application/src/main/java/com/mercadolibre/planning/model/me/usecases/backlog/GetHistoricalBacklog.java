package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.utils.DateUtils.minutesFromWeekStart;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
class GetHistoricalBacklog {
  private static final String PROCESS_KEY = "process";

  private static final int BACKLOG_WEEKS_DATE_FROM_LOOKBACK = 3;

  private static final long DAYS_OF_WEEK = 7L;

  private final BacklogPhotoApiGateway backlogPhotoApiGateway;

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
    return IntStream.rangeClosed(1, BACKLOG_WEEKS_DATE_FROM_LOOKBACK)
        .parallel()
        .mapToObj(shift -> getData(input, Duration.ofDays(DAYS_OF_WEEK * shift)))
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(Collectors.groupingBy(Map.Entry::getKey,
                                       Collectors.flatMapping(entry -> entry.getValue().entrySet().stream(),
                                                              Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
  }

  private Map<ProcessName, Map<Instant, Integer>> getData(final GetHistoricalBacklogInput input,
                                                          final Duration shift) {
    return getConsolidatedTrajectory(input, shift).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().collect(
            Collectors.toMap(BacklogPhoto::getTakenOn, BacklogPhoto::getQuantity))));
  }

  private Map<ProcessName, List<BacklogPhoto>> getConsolidatedTrajectory(final GetHistoricalBacklogInput input,
                                                                         final Duration shift) {

    final Instant adjustedRequestDate = input.getRequestDate().minus(shift);
    final Instant adjustedDateFrom = input.getDateFrom().minus(shift);
    // adds five minutes to take into account the consolidations job's delays
    final Instant adjustedDateTo = input.getDateTo().minus(shift).plus(5, MINUTES);

    final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());
    final int offsetFrom = workflow.getSlaFromOffsetInHours();
    final int offsetTo = workflow.getSlaToOffsetInHours();

    return backlogPhotoApiGateway.getTotalBacklogPerProcessAndInstantDate(
        new BacklogRequest(
            input.getWarehouseId(),
            Set.of(input.getWorkflow()),
            new HashSet<>(input.getProcesses()),
            adjustedDateFrom,
            adjustedDateTo,
            null,
            null,
            adjustedRequestDate.minus(offsetFrom, HOURS),
            adjustedRequestDate.plus(offsetTo, HOURS),
            Set.of(STEP, AREA)
        ),
        true
    );
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
}
