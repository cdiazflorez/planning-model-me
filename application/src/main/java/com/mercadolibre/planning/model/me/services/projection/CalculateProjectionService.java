package com.mercadolibre.planning.model.me.services.projection;

import static com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils.calculateProjectedEndDate;
import static com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils.calculateRemainingQuantity;
import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.DelegateAssistant;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.context.UpstreamByInflectionPoints;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateRatioSplitter;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.Value;

@Named
public class CalculateProjectionService {

  private static final int INFLECTION_POINT_DURATION = 5;

  private static final ZoneId UTC = ZoneId.of("UTC");

  private static final int NO_BACKLOG = 0;

  private static final String PACKING_PROCESS_GROUP = "packing_group";

  private static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";

  private static final List<ProcessName> OUTBOUND_PROCESSES = List.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private static final OrderedBacklogByDate EMPTY_BACKLOG = new OrderedBacklogByDate(emptyMap());

  private static final OrderedBacklogByDateRatioSplitter.Distribution<String> DEFAULT_PACKING_DISTRIBUTION =
      new OrderedBacklogByDateRatioSplitter.Distribution<>(Map.of(PACKING.getName(), 0.5, CONSOLIDATION_PROCESS_GROUP, 0.5));

  private static SequentialProcess buildProcessGraph(final Workflow workflow) {
    if (workflow == Workflow.FBM_WMS_OUTBOUND) {
      return SequentialProcess.builder()
          .name(workflow.getName())
          .process(new SimpleProcess(WAVING.getName()))
          .process(new SimpleProcess(PICKING.getName()))
          .process(
              ParallelProcess.builder()
                  .name(PACKING_PROCESS_GROUP)
                  .processor(new SimpleProcess(PACKING.getName()))
                  .processor(
                      SequentialProcess.builder()
                          .name(CONSOLIDATION_PROCESS_GROUP)
                          .process(new SimpleProcess(BATCH_SORTER.getName()))
                          .process(new SimpleProcess(WALL_IN.getName()))
                          .process(new SimpleProcess(PACKING_WALL.getName()))
                          .build()
                  )
                  .build()
          )
          .build();
    } else {
      throw new UnsupportedWorkflowException();
    }
  }

  private static Function<Instant, ZonedDateTime> projectedEndDate(final ContextsHolder projection, final Collection<Instant> dateOuts) {
    final Map<Instant, Instant> endDateByDateOut = calculateProjectedEndDate(
        projection, Workflow.FBM_WMS_OUTBOUND.getName(), new ArrayList<>(dateOuts)
    );

    return cpt -> Optional.ofNullable(endDateByDateOut.get(cpt))
        .map(instant -> ZonedDateTime.ofInstant(instant, ZoneOffset.UTC))
        .orElse(null);
  }

  private static Function<Instant, Integer> remainingBacklog(final ContextsHolder projection, final Collection<Instant> dateOuts) {
    final var processes = OUTBOUND_PROCESSES.stream()
        .map(ProcessName::getName)
        .collect(Collectors.toList());

    final Map<Instant, Long> remainingQuantityByDateOut = calculateRemainingQuantity(projection, processes, new ArrayList<>(dateOuts));

    return cpt -> Optional.ofNullable(remainingQuantityByDateOut.get(cpt))
        .map(Long::intValue)
        .orElse(0);
  }

  private static Map<Instant, Integer> toBacklogByDateOut(final List<Photo.Group> backlogGroup) {
    return backlogGroup.stream().collect(
        toMap(
            backlog -> Instant.parse(backlog.getKey().get(DATE_OUT)), Photo.Group::getTotal, Integer::sum
        )
    );
  }

  private static Map<Instant, Long> toForecastByDateOut(final List<PlanningDistributionResponse> forecastSales) {
    return forecastSales.stream().collect(
        toMap(forecast -> Instant.from(forecast.getDateOut()), PlanningDistributionResponse::getTotal, Long::sum)
    );
  }

  private static List<ProjectionResult> mappingResponseService(
      final Map<Instant, ProcessingTime> processingTimeByDateOut,
      final Function<Instant, ZonedDateTime> endDate,
      final Function<Instant, Integer> remaining,
      final Map<Instant, Integer> backlog,
      final Map<Instant, Long> forecastSales,
      final Instant dateFrom) {

    return processingTimeByDateOut.entrySet().stream().map(
        processingTime -> new ProjectionResult(
            ZonedDateTime.ofInstant(processingTime.getKey(), UTC),
            obtainProjectedEndDate(endDate, backlog, forecastSales, Instant.from(processingTime.getKey()), dateFrom),
            null,
            remaining.apply(processingTime.getKey()),
            processingTime.getValue(),
            false,
            false,
            null,
            0,
            null
        )
    ).collect(Collectors.toList());
  }

  private static ZonedDateTime obtainProjectedEndDate(
      final Function<Instant, ZonedDateTime> endDate,
      final Map<Instant, Integer> backlog,
      final Map<Instant, Long> forecastSales,
      final Instant date,
      final Instant dateFrom) {

    return backlog.getOrDefault(date, NO_BACKLOG) > NO_BACKLOG
        || forecastSales.getOrDefault(date, (long) NO_BACKLOG) > NO_BACKLOG
        ? endDate.apply(date) : dateFrom.atZone(UTC);
  }

  private static List<Instant> generateInflectionPoints(final Instant dateFrom, final Instant dateTo) {
    final Instant firstInflectionPoint = dateFrom.truncatedTo(MINUTES);
    final int currentMinute = LocalDateTime.ofInstant(firstInflectionPoint, UTC).getMinute();
    final int minutesToSecondInflectionPoint = INFLECTION_POINT_DURATION - (currentMinute % INFLECTION_POINT_DURATION);
    final Instant secondInflectionPoint = firstInflectionPoint.plus(minutesToSecondInflectionPoint, MINUTES);

    final List<Instant> inflectionPoints = new ArrayList<>();
    inflectionPoints.add(firstInflectionPoint);
    Instant date = secondInflectionPoint;

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      inflectionPoints.add(date);
      date = date.plus(INFLECTION_POINT_DURATION, MINUTES);
    }
    return inflectionPoints;
  }

  private static Map<ProcessName, Map<Instant, Integer>> getThroughput(final Map<ProcessName, Map<Instant, Integer>> throughputByProcess) {
    if (throughputByProcess.containsKey(WAVING)) {
      return throughputByProcess;
    } else {
      final var wavingTph = Stream.concat(
          throughputByProcess.get(PACKING).entrySet().stream(),
          throughputByProcess.get(PACKING_WALL).entrySet().stream()
      ).collect(toMap(
          Map.Entry::getKey,
          Map.Entry::getValue,
          Integer::sum
      ));

      return Stream.concat(
          throughputByProcess.entrySet().stream(),
          Map.of(WAVING, wavingTph).entrySet().stream()
      ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    }
  }

  private static Map<ProcessName, SimpleProcess.Context> generateSimpleProcessContexts(
      final Map<ProcessName, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess
  ) {
    final var helper = new BacklogByDateHelper(
        new OrderedBacklogByDateConsumer(),
        new OrderedBacklogByDateMerger()
    );

    final var backlogQuantityByProcess = currentBacklog.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> new OrderedBacklogByDate(
                entry.getValue()
                    .entrySet()
                    .stream()
                    .collect(toMap(
                        Map.Entry::getKey,
                        inner -> new OrderedBacklogByDate.Quantity(inner.getValue())
                    ))
            )
        ));

    final var tph = getThroughput(throughputByProcess);
    return OUTBOUND_PROCESSES.stream()
        .collect(
            toMap(
                Function.identity(),
                process -> new SimpleProcess.Context(
                    new ThroughputPerHour(tph.getOrDefault(process, emptyMap())),
                    helper,
                    backlogQuantityByProcess.getOrDefault(process, EMPTY_BACKLOG)
                )
            )
        );
  }

  private static Map<Instant, OrderedBacklogByDateRatioSplitter.Distribution<String>> ratiosAsDistributions(
      final Map<Instant, PackingRatioCalculator.PackingRatio> ratioByHour,
      final List<Instant> inflectionPoints) {
    final var baseRatios = ratioByHour.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> new OrderedBacklogByDateRatioSplitter.Distribution<>(
                Map.of(
                    PACKING.getName(), entry.getValue().getPackingToteRatio(),
                    CONSOLIDATION_PROCESS_GROUP, entry.getValue().getPackingWallRatio()
                )
            )
        ));

    return inflectionPoints.stream()
        .collect(toMap(
            Function.identity(),
            ip -> baseRatios.getOrDefault(ip.truncatedTo(ChronoUnit.HOURS), DEFAULT_PACKING_DISTRIBUTION)
        ));
  }

  /**
   * Calculate projection about params.
   *
   * @param requestDate             request date
   * @param dateFrom                request dateFrom
   * @param dateTo                  request dateTo
   * @param workflow                workflow
   * @param throughputByProcess     Throughput by process
   * @param backlogPhoto            photo's group from current backlog
   * @param forecastSales           forecast's sales with backlog mayor to 0
   * @param processingTimeByDateOut dateOut with backlog mayor or equal to 0 and processingTime
   * @param ratioByHour             ratio by hour for bifurcation between regular packing and packing wall
   * @return projections
   */

  public List<ProjectionResult> execute(
      final Instant requestDate,
      final Instant dateFrom,
      final Instant dateTo,
      final Workflow workflow,
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess,
      final List<Photo.Group> backlogPhoto,
      final List<PlanningDistributionResponse> forecastSales,
      final Map<Instant, ProcessingTime> processingTimeByDateOut,
      final Map<Instant, PackingRatioCalculator.PackingRatio> ratioByHour
  ) {

    final var currentHourRatio = ratioByHour.get(dateTo.truncatedTo(ChronoUnit.HOURS));
    final var currentBacklog = BacklogProjectionGrouper.reduce(backlogPhoto, currentHourRatio);
    final var inflectionPoints = generateInflectionPoints(requestDate, dateTo);
    final var ratios = ratiosAsDistributions(ratioByHour, inflectionPoints);

    final SequentialProcess globalSequentialProcess = buildProcessGraph(workflow);
    final ContextsHolder context = buildContextsHolder(currentBacklog, throughputByProcess, ratios);
    final UpstreamByInflectionPoints forecastedBacklog = mapForecastToUpstreamBacklog(forecastSales);

    final ContextsHolder projection = globalSequentialProcess.accept(context, forecastedBacklog, inflectionPoints);

    final Function<Instant, ZonedDateTime> endDate = projectedEndDate(projection, processingTimeByDateOut.keySet());

    final Function<Instant, Integer> remaining = remainingBacklog(projection, processingTimeByDateOut.keySet());

    return mappingResponseService(
        processingTimeByDateOut,
        endDate,
        remaining,
        toBacklogByDateOut(backlogPhoto),
        toForecastByDateOut(forecastSales),
        dateFrom
    );
  }

  private DelegateAssistant getPackingAssistant(
      final Map<Instant, OrderedBacklogByDateRatioSplitter.Distribution<String>> packingDistributionRatios) {
    return new DelegateAssistant(
        new OrderedBacklogByDateRatioSplitter(packingDistributionRatios, DEFAULT_PACKING_DISTRIBUTION),
        new OrderedBacklogByDateMerger()
    );
  }

  private ContextsHolder buildContextsHolder(
      final Map<ProcessName, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess,
      final Map<Instant, OrderedBacklogByDateRatioSplitter.Distribution<String>> packingDistributionRatios
  ) {
    final var processSimpleProcessContexts = generateSimpleProcessContexts(currentBacklog, throughputByProcess);
    final var assistant = getPackingAssistant(packingDistributionRatios);

    return ContextsHolder.builder()
        .oneProcessContext(WAVING.getName(), processSimpleProcessContexts.get(WAVING))
        .oneProcessContext(PICKING.getName(), processSimpleProcessContexts.get(PICKING))
        .oneProcessContext(BATCH_SORTER.getName(), processSimpleProcessContexts.get(BATCH_SORTER))
        .oneProcessContext(WALL_IN.getName(), processSimpleProcessContexts.get(WALL_IN))
        .oneProcessContext(PACKING.getName(), processSimpleProcessContexts.get(PACKING))
        .oneProcessContext(PACKING_WALL.getName(), processSimpleProcessContexts.get(PACKING_WALL))
        .oneProcessContext(PACKING_PROCESS_GROUP, new ParallelProcess.Context(assistant, emptyList()))
        .build();
  }

  private UpstreamByInflectionPoints mapForecastToUpstreamBacklog(final List<PlanningDistributionResponse> forecastSales) {

    final Map<Instant, Backlog> upstreamBacklog = forecastSales.stream()
        .filter(entry -> entry.getTotal() > 0)
        .collect(
            groupingBy(
                pu -> pu.getDateIn().toInstant(),
                collectingAndThen(
                    toMap(
                        pd -> pd.getDateOut().toInstant(),
                        PlanningDistributionResponse::getTotal,
                        Long::sum
                    ),
                    map -> new OrderedBacklogByDate(
                        map.entrySet()
                            .stream()
                            .collect(toMap(
                                Map.Entry::getKey,
                                entry -> new OrderedBacklogByDate.Quantity(entry.getValue())
                            ))
                    )
                )
            )
        );

    return new UpstreamByInflectionPoints(upstreamBacklog);
  }

  private static class UnsupportedWorkflowException extends RuntimeException {

    private static final long serialVersionUID = 4955135398329582928L;

    UnsupportedWorkflowException() {
      super("Workflow not supported");
    }
  }

  private static final class BacklogProjectionGrouper {

    private static final Map<Step, ProcessName> PROCESS_BY_STEP = Map.of(
        Step.PENDING, WAVING,
        Step.TO_ROUTE, PICKING,
        Step.TO_PICK, PICKING,
        Step.TO_SORT, BATCH_SORTER,
        Step.SORTED, WALL_IN,
        Step.TO_GROUP, WALL_IN,
        Step.GROUPING, WALL_IN
    );

    private static final String PACKING_WALL_AREA = "PW";

    private BacklogProjectionGrouper() {
    }

    private static ProcessName getProcess(final Photo.Group group) {
      final var step = group.getGroupValue(STEP)
          .flatMap(Step::from)
          .orElseThrow();

      if (step == Step.TO_PACK) {
        final boolean isPackingWall = group.getGroupValue(AREA)
            .map(PACKING_WALL_AREA::equalsIgnoreCase)
            .orElse(false);

        return isPackingWall ? PACKING_WALL : PACKING;
      } else {
        return PROCESS_BY_STEP.get(step);
      }
    }

    private static Stream<ProcessBacklogAtSla> toProcessBacklogAtSla(
        final Photo.Group group,
        final PackingRatioCalculator.PackingRatio ratio
    ) {

      final boolean isPicked = group.getGroupValue(STEP)
          .map(step -> step.equalsIgnoreCase(Step.PICKED.getName()))
          .orElse(false);

      final Instant sla = group.getGroupValue(DATE_OUT)
          .map(Instant::parse)
          .orElseThrow();

      if (isPicked) {
        return Stream.of(
            new ProcessBacklogAtSla(PACKING, sla, (int) (group.getTotal() * ratio.getPackingToteRatio())),
            new ProcessBacklogAtSla(BATCH_SORTER, sla, (int) (group.getTotal() * ratio.getPackingWallRatio()))
        );
      } else {
        final var process = getProcess(group);
        return Stream.of(
            new ProcessBacklogAtSla(process, sla, group.getTotal())
        );
      }
    }

    private static Map<ProcessName, Map<Instant, Integer>> reduce(
        final List<Photo.Group> photo,
        final PackingRatioCalculator.PackingRatio ratio
    ) {

      return photo.stream()
          .flatMap(group -> toProcessBacklogAtSla(group, ratio))
          .collect(groupingBy(
              ProcessBacklogAtSla::getProcess,
              toMap(
                  ProcessBacklogAtSla::getSla,
                  ProcessBacklogAtSla::getQuantity,
                  Integer::sum
              )
          ));
    }
  }

  @Value
  private static class ProcessBacklogAtSla {
    ProcessName process;

    Instant sla;

    int quantity;
  }
}
