package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.emptyMeasure;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromMinutes;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import static java.lang.Math.max;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail.SubAreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.Headcount;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitorDetails extends GetConsolidatedBacklog {

  private static final int HOUR_TPH_FUTURE = 24;

  private final PlanningModelGateway planningModelGateway;

  private final GetProcessThroughput getProcessThroughput;

  private final GetHistoricalBacklog getHistoricalBacklog;

  private final GetBacklogLimits getBacklogLimits;

  private final List<BacklogProvider> backlogProviders;

  public GetBacklogMonitorDetailsResponse execute(final GetBacklogMonitorDetailsInput input) {
    final List<VariablesPhoto> backlog = getData(input);

    final Instant currentDatetime = getDateOfLatestNonProjectionBacklogPhoto(backlog, input.getRequestDate());

    final List<AreaName> areas = input.getProcess().hasAreas() ? areasPresentInBacklog(backlog) : emptyList();

    return new GetBacklogMonitorDetailsResponse(
        currentDatetime,
        getBacklogDetails(backlog, areas, currentDatetime),
        getResumedBacklog(input.getProcess(), currentDatetime, backlog, input.getDateFrom())
    );
  }

  private List<AreaName> areasPresentInBacklog(final List<VariablesPhoto> backlog) {
    final Map<String, Set<String>> subareasByArea = backlog.stream()
        .map(VariablesPhoto::getAreas)
        .flatMap(entry -> entry.values().stream())
        .collect(
            Collectors.groupingBy(
                NumberOfUnitsInAnArea::getArea,
                Collectors.flatMapping(
                    allAreas -> allAreas.getSubareas()
                        .stream()
                        .map(NumberOfUnitsInASubarea::getName),
                    Collectors.toCollection(TreeSet::new)
                )
            )
        );

    return subareasByArea.entrySet()
        .stream()
        .map(entry -> new AreaName(entry.getKey(), entry.getValue()))
        .sorted(comparing(AreaName::getName))
        .collect(Collectors.toList());

  }

  private Instant getDateOfLatestNonProjectionBacklogPhoto(final List<VariablesPhoto> backlog, final Instant requestInstant) {
    return backlog.stream()
        .filter(stats -> !stats.isProjection())
        .map(VariablesPhoto::getDate)
        .max(Comparator.naturalOrder())
        .orElse(requestInstant);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getBacklog(final GetBacklogMonitorDetailsInput input,
                                                               final GetThroughputResult throughput) {

    final var providerInput = new BacklogProvider.BacklogProviderInput(
        input.getRequestDate(),
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getProcess(),
        throughput.asMagnitudePhotos(),
        input.getDateFrom(),
        input.getDateTo(),
        null,
        null,
        input.getCallerId(),
        input.isHasWall()
    );

    return backlogProviders.stream()
        .filter(provider -> provider.canProvide(input.getProcess()))
        .findFirst()
        .map(provider -> provider.getMonitorBacklog(providerInput))
        .orElseThrow();
  }

  private List<VariablesPhoto> getData(final GetBacklogMonitorDetailsInput input) {
    final GetThroughputResult throughput = getThroughput(input);
    final Map<Instant, Integer> throughputByDate = getProcessThroughputByDate(throughput, input.getProcess());

    final Map<Instant, BacklogLimit> limits = getBacklogLimits(input);
    final Map<Instant, Integer> targetBacklog = getTargetBacklog(input);

    final Map<Instant, List<NumberOfUnitsInAnArea>> backlog = getBacklog(input, throughput);
    final HistoricalBacklog historicalBacklog = getHistoricalBacklog(input);

    final Map<Instant, UnitMeasure> backlogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(toTotaledBacklogPhoto(backlog), null, throughputByDate);

    final var viewDate = input.getRequestDate();

    return backlog.entrySet()
        .stream()
        .map(entry -> toProcessStats(
                 entry.getKey().isAfter(viewDate),
                 entry.getKey(),
                 entry.getValue(),
                 targetBacklog,
                 historicalBacklog,
                 throughputByDate,
                 limits,
                 backlogMeasuredInHours
             )
        )
        .collect(Collectors.toList());
  }

  private VariablesPhoto toProcessStats(final boolean isProjection,
                                        final Instant date,
                                        final List<NumberOfUnitsInAnArea> areas,
                                        final Map<Instant, Integer> targetBacklog,
                                        final HistoricalBacklog historicalBacklog,
                                        final Map<Instant, Integer> throughput,
                                        final Map<Instant, BacklogLimit> limits,
                                        final Map<Instant, UnitMeasure> backlogMeasuredInHours) {

    final UnitMeasure total = backlogMeasuredInHours.getOrDefault(date, emptyMeasure());
    final Instant truncatedDate = date.truncatedTo(ChronoUnit.HOURS);
    final int tphDefault = throughput.getOrDefault(truncatedDate, 0);

    final int tphAverage = getTphAverage(total);
    final int throughputValue = tphAverage == 0 ? tphDefault : tphAverage;

    final UnitMeasure target = Optional.ofNullable(targetBacklog.get(truncatedDate))
        .map(t -> UnitMeasure.fromUnits(t, throughputValue))
        .orElse(null);

    final BacklogLimit limit = limits.get(truncatedDate);

    final UnitMeasure min = limit == null || limit.getMin() < 0 ? emptyMeasure() : fromMinutes(limit.getMin(), throughputValue);
    final UnitMeasure max = limit == null || limit.getMax() < 0 ? emptyMeasure() : fromMinutes(limit.getMax(), throughputValue);

    final Map<String, Integer> unitsByArea = areas.stream()
        .collect(
            Collectors.toMap(
                NumberOfUnitsInAnArea::getArea,
                backlog -> max(0, backlog.getUnits()),
                Integer::sum
            )
        );

    final Map<String, NumberOfUnitsInAnArea> areasByName = areas.stream()
        .collect(
            Collectors.toMap(
                NumberOfUnitsInAnArea::getArea,
                Function.identity()
            )
        );

    return new VariablesPhoto(
        isProjection,
        date,
        total,
        target,
        min,
        max,
        throughputValue,
        historicalBacklog.getOr(truncatedDate, UnitMeasure::emptyMeasure),
        unitsByArea,
        areasByName);
  }

  private Map<Instant, Integer> getTargetBacklog(final GetBacklogMonitorDetailsInput input) {

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

  private GetThroughputResult getThroughput(final GetBacklogMonitorDetailsInput input) {
    final var processes = input.getProcess() == PICKING ? of(ProcessName.WAVING, PICKING) : of(input.getProcess());
    final GetThroughputInput request = GetThroughputInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processes(processes)
        .dateFrom(input.getRequestDate().atZone(UTC))
        .dateTo(input.getDateTo().atZone(UTC).plusHours(HOUR_TPH_FUTURE))
        .build();

    try {
      return getProcessThroughput.execute(request);
    } catch (RuntimeException e) {
      log.error("could not retrieve throughput for {}", request, e);
    }
    return GetThroughputResult.emptyThroughput();
  }

  private Map<Instant, Integer> getProcessThroughputByDate(final GetThroughputResult result, final ProcessName process) {
    return result.getOrDefault(process, Map.of())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey().toInstant(), Map.Entry::getValue));
  }

  private HistoricalBacklog getHistoricalBacklog(final GetBacklogMonitorDetailsInput input) {
    return getHistoricalBacklog.execute(
        new GetHistoricalBacklogInput(
            input.getRequestDate(),
            input.getWarehouseId(),
            input.getWorkflow(),
            of(input.getProcess()),
            input.getDateFrom(),
            input.getDateTo())).get(input.getProcess());
  }

  private Map<Instant, BacklogLimit> getBacklogLimits(final GetBacklogMonitorDetailsInput input) {
    try {
      return getBacklogLimits.execute(
              GetBacklogLimitsInput.builder()
                  .warehouseId(input.getWarehouseId())
                  .workflow(input.getWorkflow())
                  .processes(of(input.getProcess()))
                  .dateFrom(input.getDateFrom())
                  .dateTo(input.getDateTo())
                  .build()
          )
          .getOrDefault(input.getProcess(), emptyMap());

    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return emptyMap();
  }

  private List<DetailedBacklogPhoto> getBacklogDetails(
     final List<VariablesPhoto> backlog,
     final List<AreaName> areas,
     final Instant currentDatetime
  ) {
    return backlog.stream()
        .map(b -> this.toProcessDetail(b, areas, currentDatetime))
        .sorted(comparing(DetailedBacklogPhoto::getDate))
        .collect(Collectors.toList());
  }

  private ProcessDetail getResumedBacklog(
      final ProcessName process,
      final Instant currentDatetime,
      final List<VariablesPhoto> variablesTrajectory,
      final Instant dateFrom
  ) {

    return build(
        process,
        currentDatetime,
        variablesTrajectory.stream()
            .map(current -> new BacklogStatsByDate(
                current.getDate(),
                current.getTotal(),
                current.getHistorical(),
                current.getMinLimit(),
                current.getMaxLimit())).collect(Collectors.toList()),
                dateFrom);
  }

  private DetailedBacklogPhoto toProcessDetail(final VariablesPhoto variablesPhoto,
                                               final List<AreaName> processAreas,
                                               final Instant currentDatetime) {

    final UnitMeasure totalBacklog = variablesPhoto.getTotal();
    final UnitMeasure targetBacklog = variablesPhoto.getTarget();
    final Integer totalReps = variablesPhoto.getAreas()
        .values()
        .stream()
        .map(NumberOfUnitsInAnArea::getReps)
        .filter(Objects::nonNull)
        .reduce(0, Integer::sum);

    final Double percentage = variablesPhoto.getAreas()
        .values()
        .stream()
        .map(NumberOfUnitsInAnArea::getRepsPercentage)
        .filter(Objects::nonNull)
        .reduce(0.0, Double::sum);

    final Headcount headcount = new Headcount(totalReps, percentage);

    final List<AreaBacklogDetail> areas = processAreas.isEmpty()
        ? null
        : toAreas(variablesPhoto, processAreas);

    final Instant date = variablesPhoto.getDate().equals(currentDatetime)
        ? currentDatetime
        : variablesPhoto.getDate().truncatedTo(ChronoUnit.HOURS);

    return new DetailedBacklogPhoto(date, headcount, targetBacklog, totalBacklog, areas);
  }

  private AreaBacklogDetail mapBacklogAreaDetail(final AreaName area,
                                                 final NumberOfUnitsInAnArea unitsInThisArea,
                                                 final Integer throughput) {
    final Optional<NumberOfUnitsInAnArea> numberOfUnitsInAnArea = Optional.ofNullable(unitsInThisArea);

    final List<SubAreaBacklogDetail> mappedSubareas = area.getSubareas()
        .stream()
        .map(subarea -> {
               final Optional<NumberOfUnitsInASubarea> unitsInSubArea = numberOfUnitsInAnArea.flatMap(subareas -> subareas.getSubareas()
                   .stream()
                   .filter(s -> s.getName().equals(subarea))
                   .findFirst()
               );

               return unitsInSubArea.map(value ->
                                             new SubAreaBacklogDetail(
                                                 subarea,
                                                 UnitMeasure.fromUnits(value.getUnits(), throughput),
                                                 new Headcount(value.getReps() != null ? value.getReps() : 0,
                                                               value.getRepsPercentage() != null ? value.getRepsPercentage() : 0D))
                   )
                   .orElseGet(() -> new SubAreaBacklogDetail(subarea, emptyMeasure(), new Headcount(0, 0.0)));
             }
        )
        .collect(Collectors.toList());


    final Integer totalUnitsInArea = numberOfUnitsInAnArea.map(NumberOfUnitsInAnArea::getUnits).orElse(0);
    final UnitMeasure measure = UnitMeasure.fromUnits(totalUnitsInArea, throughput);

    Headcount headcountArea = numberOfUnitsInAnArea.map(value -> new Headcount(
            value.getReps() != null ? value.getReps() : 0,
            value.getRepsPercentage() != null ? value.getRepsPercentage() : 0D))
        .orElse(new Headcount(0, 0D));

    return new AreaBacklogDetail(area.getName(), measure, headcountArea, mappedSubareas.size() == 1 ? null : mappedSubareas);
  }

  private List<AreaBacklogDetail> toAreas(final VariablesPhoto variablesPhoto, final List<AreaName> areas) {
    final Integer throughput = variablesPhoto.getThroughput();
    final Map<String, NumberOfUnitsInAnArea> currentPhotoAreas = variablesPhoto.getAreas();

    return areas.stream()
        .map(area -> mapBacklogAreaDetail(area, currentPhotoAreas.get(area.getName()), throughput))
        .collect(Collectors.toList());
  }

  private List<TotaledBacklogPhoto> toTotaledBacklogPhoto(final Map<Instant, List<NumberOfUnitsInAnArea>> totaledBacklogByArea) {
    return totaledBacklogByArea.entrySet().stream()
        .map(entry -> new TotaledBacklogPhoto(entry.getKey(), entry.getValue().stream().map(NumberOfUnitsInAnArea::getUnits)
            .reduce(0, Integer::sum))).collect(Collectors.toList());
  }

  private int getTphAverage(final UnitMeasure backlogMeasuredInHour) {
    double avgByHour = (double) backlogMeasuredInHour.getMinutes() / 60;
    return avgByHour > 0 ? (int) ((double) backlogMeasuredInHour.getUnits() / avgByHour) : 0;
  }

  public interface BacklogProvider {
    boolean canProvide(ProcessName process);

    Map<Instant, List<NumberOfUnitsInAnArea>> getMonitorBacklog(BacklogProviderInput input);

    @Value
    class BacklogProviderInput {
      Instant requestDate;

      String warehouseId;

      Workflow workflow;

      ProcessName process;

      List<MagnitudePhoto> throughput;

      Instant dateFrom;

      Instant dateTo;

      Instant slaFrom;

      Instant slaTo;

      Long callerId;

      boolean hasWall;
    }
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

    Map<String, NumberOfUnitsInAnArea> areas;
  }

  @Value
  private static class AreaName {
    String name;

    Set<String> subareas;
  }
}
