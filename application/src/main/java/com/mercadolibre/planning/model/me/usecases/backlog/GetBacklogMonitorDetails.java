package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.emptyMeasure;
import static com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure.fromMinutes;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.monitor.AreaBacklogDetail;
import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogStatsByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsResponse;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMonitorDetails extends GetConsolidatedBacklog {
  private static final String NO_AREA = "N/A";

  private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
      FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING),
      FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
  );

  private static final int HOUR_TPH_FUTURE = 24;

  private final BacklogApiAdapter backlogApiAdapter;

  private final PlanningModelGateway planningModelGateway;

  private final GetProcessThroughput getProcessThroughput;

  private final GetHistoricalBacklog getHistoricalBacklog;

  private final GetBacklogLimits getBacklogLimits;

  private final ProjectBacklog projectBacklog;

  public GetBacklogMonitorDetailsResponse execute(final GetBacklogMonitorDetailsInput input) {
    final List<VariablesPhoto> backlog = getData(input);

    final Instant currentDatetime = getDateOfLatestNonProjectionBacklogPhoto(backlog, input.getRequestDate());

    final List<String> areas = input.getProcess().hasAreas() ? areas(backlog) : emptyList();

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

  private List<BacklogGrouper> getBacklogGroupers(final ProcessName process) {
    // only picking has areas enabled
    return process.hasAreas() ? of(PROCESS, AREA, DATE_OUT) : of(PROCESS);
  }

  private List<ProcessName> getQueryProcesses(final ProcessName process) {
    // to apply the new backlog projection we need to retrieve both waving and picking tph and backlogs
    return process == PICKING ? of(WAVING, PICKING) : of(process);
  }

  private List<VariablesPhoto> getData(final GetBacklogMonitorDetailsInput input) {
    final BacklogWorkflow workflow = BacklogWorkflow.from(input.getWorkflow());

    final List<Consolidation> currentBacklog = backlogApiAdapter.getCurrentBacklog(
        input.getRequestDate(),
        input.getWarehouseId(),
        of(input.getWorkflow()),
        getQueryProcesses(input.getProcess()),
        getBacklogGroupers(input.getProcess()),
        input.getDateFrom(),
        input.getDateTo(),
        input.getRequestDate().minus(workflow.getSlaFromOffsetInHours(), ChronoUnit.HOURS),
        input.getRequestDate().plus(workflow.getSlaToOffsetInHours(), ChronoUnit.HOURS));

    final GetThroughputResult throughput = getThroughput(input);
    final Map<Instant, Integer> throughputByDate = getProcessThroughputByDate(throughput, input.getProcess());
    final Map<Instant, BacklogLimit> limits = getBacklogLimits(input);
    final Map<Instant, Integer> targetBacklog = getTargetBacklog(input);
    final HistoricalBacklog historicalBacklog = getHistoricalBacklog(input);
    final Map<Instant, List<NumberOfUnitsInAnArea>> historicBacklog = getPastBacklog(input, currentBacklog);
    final Map<Instant, List<NumberOfUnitsInAnArea>> projectedBacklog =
        getProjectedBacklog(input, currentBacklog, throughput.asMagnitudePhotos());

    final Map<Instant, UnitMeasure> currentBacklogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(toTotaledBacklogPhoto(historicBacklog), throughputByDate);
    final Map<Instant, UnitMeasure> projectedBacklogMeasuredInHours =
        convertBacklogTrajectoryFromUnitToTime(toTotaledBacklogPhoto(projectedBacklog), throughputByDate);

    return Stream.concat(
        historicBacklog.entrySet()
            .stream()
            .map(entry ->
                toProcessStats(
                    false,
                    entry.getKey(),
                    entry.getValue(),
                    targetBacklog,
                    historicalBacklog,
                    throughputByDate,
                    limits,
                    currentBacklogMeasuredInHours)),
        projectedBacklog.entrySet()
            .stream()
            .map(entry ->
                toProcessStats(
                    true,
                    entry.getKey(),
                    entry.getValue(),
                    targetBacklog,
                    historicalBacklog,
                    throughputByDate,
                    limits,
                    projectedBacklogMeasuredInHours))
    ).collect(Collectors.toList());
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
                backlog -> backlog.getUnits() >= 0 ? backlog.getUnits() : 0,
                Integer::sum
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
        unitsByArea);
  }

  private boolean isCurrentProcessBacklog(final Consolidation consolidation, final ProcessName processName) {
    return Optional.of(consolidation)
        .map(Consolidation::getKeys)
        .map(keys -> keys.get("process"))
        .map(process -> process.equals(processName.getName()))
        .orElse(true);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getPastBacklog(final GetBacklogMonitorDetailsInput input,
                                                                   final List<Consolidation> currentBacklog) {

    final List<Consolidation> filteredConsolidations = currentBacklog.stream()
        .filter(consolidation -> isCurrentProcessBacklog(consolidation, input.getProcess()))
        .collect(Collectors.toList());

    final List<Consolidation> fixedConsolidation = fixBacklog(
        input.getRequestDate(),
        filteredConsolidations,
        input.getDateFrom(),
        date -> new Consolidation(date, Map.of("area", NO_AREA), 0, true)
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

    final List<Consolidation> sumsOfCellsGroupedByTakenOnDateAndProcessOnTheDot =
        filterSumsOfCellByTakenOnTheDot(consolidation);
    final Instant latestPhotoDate = getDateWhenLatestPhotoWasTaken(sumsOfCellsGroupedByTakenOnDateAndProcessOnTheDot, requestDate);
    final List<Consolidation> truncatedConsolidations = truncateToHoursTheTakenOnDate(sumsOfCellsGroupedByTakenOnDateAndProcessOnTheDot);
    return fillMissing(truncatedConsolidations, dateFrom, latestPhotoDate, backlogSupplier);
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(final GetBacklogMonitorDetailsInput input,
                                                                        final List<Consolidation> backlog,
                                                                        final List<MagnitudePhoto> throughput) {

    final Instant lastConsolidationDate = getDateWhenLatestPhotoWasTaken(backlog, input.getRequestDate());

    final List<Consolidation> currentBacklog = backlog.stream()
        .filter(consolidation -> consolidation.getDate().equals(lastConsolidationDate))
        .collect(Collectors.toList());

    try {
      if (input.getProcess() == PICKING) {
        return getProjectedBacklogByArea(input, currentBacklog, throughput);
      } else {
        return getProjectedBacklogWithoutAreas(input, currentBacklog);
      }
    } catch (RuntimeException e) {
      log.error("could not retrieve backlog projections", e);
    }

    return emptyMap();
  }

  private List<NumberOfUnitsInAnArea> toUnitsInArea(final List<ProjectedBacklogForAnAreaAndOperatingHour> projections) {
    Map<String, List<NumberOfUnitsInASubarea>> subareas = projections.stream()
        .collect(
            Collectors.groupingBy(
                projection -> projection.getArea().substring(0, 2),
                Collectors.mapping(
                    projection -> new NumberOfUnitsInASubarea(projection.getArea(), projection.getQuantity().intValue()),
                    Collectors.toList()
                )
            )
        );

    return subareas.entrySet()
        .stream()
        .map(entry -> new NumberOfUnitsInAnArea(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogByArea(final GetBacklogMonitorDetailsInput input,
                                                                              final List<Consolidation> currentBacklog,
                                                                              final List<MagnitudePhoto> throughput) {

    final List<ProjectedBacklogForAnAreaAndOperatingHour> projections = projectBacklog.projectBacklogInAreas(
        input.getRequestDate(),
        input.getDateTo(),
        input.getWarehouseId(),
        input.getWorkflow(),
        List.of(WAVING, PICKING),
        currentBacklog,
        throughput
    );

    final Map<Instant, List<ProjectedBacklogForAnAreaAndOperatingHour>> groupedProjections = projections.stream()
        .filter(projection -> projection.getStatus() == BacklogProcessStatus.CARRY_OVER
            && projection.getProcess() == PICKING
            && projection.getOperatingHour().isAfter(input.getRequestDate()))
        .collect(
            Collectors.groupingBy(
                ProjectedBacklogForAnAreaAndOperatingHour::getOperatingHour,
                Collectors.toList()
            )
        );

    return groupedProjections.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> toUnitsInArea(entry.getValue())
            ));
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogWithoutAreas(final GetBacklogMonitorDetailsInput input,
                                                                                    final List<Consolidation> currentBacklog) {

    final Instant dateFrom = input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

    final Instant dateTo = input.getDateTo()
        .truncatedTo(ChronoUnit.HOURS)
        .minus(1L, ChronoUnit.HOURS);

    final List<BacklogProjectionResponse> projectedBacklog = backlogApiAdapter
        .getProjectedBacklog(
            input.getWarehouseId(),
            input.getWorkflow(),
            PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
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

  private GetThroughputResult getThroughput(final GetBacklogMonitorDetailsInput input) {
    final GetThroughputInput request = GetThroughputInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processes(getQueryProcesses(input.getProcess()))
        .dateFrom(input.getDateFrom().atZone(UTC))
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
              UnitMeasure.fromUnits(units, throughput));
        }).collect(Collectors.toList());
  }

  private NumberOfUnitsInAnArea backlogToAreas(final Consolidation consolidation) {
    return new NumberOfUnitsInAnArea(
        consolidation.getKeys().get("area"),
        consolidation.getTotal()
    );
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
