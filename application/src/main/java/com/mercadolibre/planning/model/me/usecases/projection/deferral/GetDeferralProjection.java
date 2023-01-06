package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import static com.mercadolibre.planning.model.me.enums.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createColumnHeaders;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createData;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.DeferralResultData;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.monitoring.EndDayDeferralCard;
import com.mercadolibre.planning.model.me.entities.projection.monitoring.Monitoring;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.clock.RequestClockGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantity;
import com.mercadolibre.planning.model.me.gateways.projection.deferral.DeferralProjectionStatus;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectionDataMapper;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionDataInput;
import com.mercadolibre.planning.model.me.utils.MetricsService;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Named
@Slf4j
@AllArgsConstructor
public class GetDeferralProjection implements UseCase<GetProjectionInput, PlanningView> {
  private static final Boolean PROJECTIONS_VERSION = true;

  private static final int DEFERRAL_DAYS_TO_PROJECT = 3;

  private static final int DEFERRAL_DAYS_TO_SHOW = 1;

  private static final int SELECTOR_DAYS_TO_SHOW = 2;

  private static final int HOURS_TO_SHOW = 25;

  private static final String PROJECTION_TYPE = "deferral";

  private static final Map<MagnitudeType, Map<String, List<String>>> FILTER_CAP_MAX = Map.of(
      HEADCOUNT, Map.of(
          PROCESSING_TYPE.toJson(),
          List.of(MAX_CAPACITY.getName())
      )
  );

  private static final List<String> CAP5_TO_PACK_STATUSES = List.of("pending", "to_route",
      "to_pick", "picked", "to_sort", "sorted", "to_group", "grouping", "grouped", "to_pack");

  private final PlanningModelGateway planningModelGateway;

  private final GetSimpleDeferralProjection getSimpleDeferralProjection;

  private final BacklogApiGateway backlogGateway;

  private final RequestClockGateway requestClockGateway;

  private final ProjectionGateway projectionGateway;

  private final MetricsService metricsService;

  private final LogisticCenterGateway logisticCenterGateway;


  @Override
  public PlanningView execute(final GetProjectionInput input) {

    final ZonedDateTime requestDateTime = ZonedDateTime.ofInstant(requestClockGateway.now(), UTC);

    final ZonedDateTime dateFromToProject = requestDateTime.truncatedTo(ChronoUnit.HOURS);

    final ZonedDateTime dateToToProject = dateFromToProject.plusDays(DEFERRAL_DAYS_TO_PROJECT);

    final Instant requestDate = requestDateTime.truncatedTo(ChronoUnit.MINUTES).toInstant();

    try {

      final boolean isSameDayHour = isSameDayHour(input.getDate(), requestDate);
      final ZonedDateTime dateFromToShow = isSameDayHour
          ? ZonedDateTime.ofInstant(requestDate, UTC)
          : input.getDate();

      final ZonedDateTime dateToToShow = dateFromToShow.plusDays(DEFERRAL_DAYS_TO_SHOW);

      final List<Backlog> backlogsToProject = getBacklog(
          input.getLogisticCenterId(),
          dateFromToProject,
          dateToToProject);

      final GetSimpleDeferralProjectionOutput deferralBaseOutput = getSimpleProjection(backlogsToProject, input);

      final List<DeferralProjectionStatus> itemsToDeferral = getItemsToDeferral(
          dateFromToProject,
          dateToToProject,
          input.getWorkflow(),
          deferralBaseOutput,
          input.getSimulations(),
          backlogsToProject,
          input.getLogisticCenterId()
      );

      final List<Backlog> backlogsToShow = filterBacklogsInRange(
          dateFromToShow,
          dateToToShow,
          backlogsToProject
      );

      final List<ProjectionResult> projectionsToShow = getProjectionsToShow(
          dateFromToShow,
          dateToToShow,
          deferralBaseOutput,
          itemsToDeferral,
          input.getWorkflow(),
          input.getLogisticCenterId()
      );

      return PlanningView.builder()
          .currentDate(now().withZoneSameInstant(UTC).truncatedTo(ChronoUnit.SECONDS))
          .dateSelector(getDateSelector(dateFromToProject, dateFromToShow, SELECTOR_DAYS_TO_SHOW))
          .data(new DeferralResultData(
              getThroughput(deferralBaseOutput.getConfiguration(),
                  input,
                  dateFromToShow.truncatedTo(ChronoUnit.HOURS),
                  dateToToShow),
              getDataProjection(input,
                  dateFromToShow,
                  dateToToShow,
                  backlogsToShow,
                  projectionsToShow),
              getDataMonitoring(
                  getMonitoringEndDayDeferralCard(itemsToDeferral, requestDateTime, deferralBaseOutput.getConfiguration().getTimeZone()))
          ))
          .build();

    } catch (RuntimeException ex) {
      log.error(ex.getMessage(), ex);
      metricsService.trackProjectionError(input.getLogisticCenterId(), input.getWorkflow(), PROJECTION_TYPE, "general");
      return PlanningView.builder()
          .currentDate(now().withZoneSameInstant(UTC).truncatedTo(ChronoUnit.SECONDS))
          .dateSelector(getDateSelector(
              requestDateTime,
              input.getDate() != null
                  ? input.getDate()
                  : requestDateTime,
              SELECTOR_DAYS_TO_SHOW))
          .emptyStateMessage(ex.getMessage())
          .build();
    }
  }

  private Monitoring getDataMonitoring(
      final EndDayDeferralCard endDayDeferralCard
  ) {
    return new Monitoring(endDayDeferralCard);
  }

  private EndDayDeferralCard getMonitoringEndDayDeferralCard(
      final List<DeferralProjectionStatus> itemsToDeferral,
      final ZonedDateTime dateFromToShow,
      final TimeZone logisticCenterZoneId
  ) {

    return new EndDayDeferralCard(itemsToDeferral.stream()
        .filter(deferralProjectionStatus -> (
                !ZonedDateTime.ofInstant(deferralProjectionStatus.getDeferredAt(), UTC).isBefore(dateFromToShow)
                    && !ZonedDateTime.ofInstant(deferralProjectionStatus.getDeferredAt(), UTC).isAfter(
                    dateFromToShow.withZoneSameInstant(logisticCenterZoneId.toZoneId()).with(LocalTime.MAX).withZoneSameInstant(UTC))
            )
        )
        .mapToInt(DeferralProjectionStatus::getDeferredUnits)
        .sum(),
        dateFromToShow
    );
  }

  private List<ProjectionResult> getProjectionsToShow(
      final ZonedDateTime dateFromToShow,
      final ZonedDateTime dateToToShow,
      final GetSimpleDeferralProjectionOutput deferralBaseOutput,
      final List<DeferralProjectionStatus> itemsToDeferral,
      final Workflow workflow,
      final String logisticCenterId
  ) {
    try {
      return filterProjectionsInRange(
          dateFromToShow,
          dateToToShow,
          deferralBaseOutput.getProjections()
      ).map(buildProjectionResultMapper(itemsToDeferral))
          .collect(toList());
    } catch (RuntimeException ex) {
      log.error("Failed getting projections to show\n" + ex.getMessage(), ex);
      metricsService.trackProjectionError(logisticCenterId, workflow, PROJECTION_TYPE, "projections_to_sow");
    }
    return Collections.emptyList();
  }

  private List<DeferralProjectionStatus> getItemsToDeferral(
      final ZonedDateTime dateFromToProject,
      final ZonedDateTime dateToToProject,
      final Workflow workflow,
      final GetSimpleDeferralProjectionOutput deferralBaseOutput,
      final List<Simulation> simulations,
      final List<Backlog> backlogsToProject,
      final String logisticCenterId
  ) {
    try {
      return projectionGateway.getDeferralProjectionStatus(
          dateFromToProject.toInstant(),
          dateToToProject.toInstant(),
          workflow,
          List.of(PACKING, PACKING, PACKING_WALL),
          backlogsToProject.stream()
              .map(backlog -> new BacklogQuantity(backlog.getDate().toInstant(), backlog.getQuantity()))
              .collect(toList()),
          logisticCenterId,
          deferralBaseOutput.getConfiguration().getTimeZone().getID(),
          true,
          simulations);
    } catch (RuntimeException ex) {
      log.error("Failed getting items to deferral\n" + ex.getMessage(), ex);
      metricsService.trackProjectionError(logisticCenterId, workflow, PROJECTION_TYPE, "items_to_deferral");
    }
    return Collections.emptyList();
  }

  private GetSimpleDeferralProjectionOutput getSimpleProjection(List<Backlog> backlogs, GetProjectionInput input) {

    GetSimpleDeferralProjectionOutput deferralProjections = getSimpleDeferralProjection.execute(
        new GetProjectionInput(
            input.getLogisticCenterId(),
            input.getWorkflow(),
            input.getDate(),
            backlogs,
            input.isWantToSimulate21(),
            input.getSimulations()));

    if (input.getSimulations() == null || input.getSimulations().isEmpty()) {
      return deferralProjections;
    } else {
      GetSimpleDeferralProjectionOutput deferralProjectionsWithoutSimulations = getSimpleDeferralProjection.execute(
          new GetProjectionInput(
              input.getLogisticCenterId(),
              input.getWorkflow(),
              input.getDate(),
              backlogs,
              input.isWantToSimulate21(),
              Collections.emptyList()));

      return new GetSimpleDeferralProjectionOutput(
          unifyProjections(deferralProjectionsWithoutSimulations.getProjections(), deferralProjections.getProjections()),
          deferralProjections.getConfiguration()
      );

    }
  }

  /**
   * WARNING: the first parameter is mutated.
   *
   * @param normalProjections     the projection we want to initialize its `simulatedEndDate` field
   * @param simulationProjections the projection whose `projectionEndDate` field we want to copy to the normalProjection's
   *                              `simulatedEndDate` field.
   * @return the normalProjections parameter mutated
   */
  private List<ProjectionResult> unifyProjections(List<ProjectionResult> normalProjections,
                                                  List<ProjectionResult> simulationProjections) {

    return normalProjections.stream().peek(projectionResult -> {
      ZonedDateTime simulatedEndDate =
          simulationProjections.stream().filter(simulation -> simulation.getDate().isEqual(projectionResult.getDate()))
              .findFirst().orElse(new ProjectionResult()).getProjectedEndDate();

      projectionResult.setSimulatedEndDate(simulatedEndDate);

    }).collect(toList());

  }

  private Function<ProjectionResult, ProjectionResult> buildProjectionResultMapper(
      List<DeferralProjectionStatus> deferralProjectionStatusList) {
    return p -> {
      DeferralProjectionStatus status = deferralProjectionStatusList.stream()
          .filter(deferralProjectionStatus -> deferralProjectionStatus.getSla().equals(p.getDate().toInstant()))
          .findFirst()
          .orElse(new DeferralProjectionStatus(null, null, 0, null));
      return new ProjectionResult(
          p.getDate(),
          p.getProjectedEndDate(),
          p.getSimulatedEndDate(),
          p.getRemainingQuantity(),
          p.getProcessingTime(),
          p.isDeferred(),
          p.isExpired(),
          status.getDeferredAt(),
          status.getDeferredUnits(),
          status.getDeferralStatus()
      );
    };
  }

  private List<Projection> getDataProjection(final GetProjectionInput input,
                                             final ZonedDateTime dateFrom,
                                             final ZonedDateTime dateTo,
                                             final List<Backlog> backlogs,
                                             final List<ProjectionResult> projection) {
    return ProjectionDataMapper.map(GetProjectionDataInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getLogisticCenterId())
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .sales(Collections.emptyList())
        .projections(projection)
        .planningDistribution(Collections.emptyList())
        .backlogs(backlogs)
        .showDeviation(false)
        .build());
  }

  private boolean isSameDayHour(final ZonedDateTime inputDate, final Instant requestDate) {
    if (inputDate == null) {
      return true;
    } else {
      final Instant selectedDate = inputDate.truncatedTo(ChronoUnit.HOURS).toInstant();
      final Instant currentDate = requestDate.truncatedTo(ChronoUnit.HOURS);
      return selectedDate.equals(currentDate);
    }
  }

  private ComplexTable getThroughput(final LogisticCenterConfiguration config,
                                     final GetProjectionInput input,
                                     final ZonedDateTime dateFrom,
                                     final ZonedDateTime dateTo) {

    final Map<MagnitudeType, List<MagnitudePhoto>> entities = planningModelGateway.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .warehouseId(input.getLogisticCenterId())
            .workflow(FBM_WMS_OUTBOUND)
            .entityTypes(List.of(HEADCOUNT, THROUGHPUT))
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .processName(List.of(GLOBAL, PACKING, PACKING_WALL))
            .entityFilters(FILTER_CAP_MAX)
            .source(Source.SIMULATION)
            .simulations(input.getSimulations())
            .build()
    );

    final List<MagnitudePhoto> maxCapacity = entities.get(HEADCOUNT);

    final List<MagnitudePhoto> throughput = entities.get(THROUGHPUT);

    final List<ColumnHeader> headers = createColumnHeaders(
        convertToTimeZone(config.getZoneId(), dateFrom), HOURS_TO_SHOW);

    return new ComplexTable(
        headers,
        List.of(createData(config, MagnitudeType.MAX_CAPACITY, maxCapacity.stream()
            .map(entity -> EntityRow.fromEntity(entity, validateCapacity(entity, throughput)))
            .collect(toList()), headers)
        ),
        action,
        ""
    );
  }

  private boolean validateCapacity(MagnitudePhoto capacity, List<MagnitudePhoto> throughput) {
    return capacity.getValue() >= throughput.stream()
        .filter(magnitudePhoto -> magnitudePhoto.getDate().isEqual(capacity.getDate()))
        .mapToInt(MagnitudePhoto::getValue)
        .sum();
  }

  private Stream<ProjectionResult> filterProjectionsInRange(
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo,
      final List<ProjectionResult> projections) {

    return projections.stream().filter(p ->
        (p.getDate().isEqual(dateFrom) || p.getDate().isAfter(dateFrom))
            && (p.getDate().isEqual(dateTo) || p.getDate().isBefore(dateTo)));
  }

  private List<Backlog> filterBacklogsInRange(final ZonedDateTime dateFrom,
                                              final ZonedDateTime dateTo,
                                              final List<Backlog> backlogs) {

    return backlogs.stream().filter(p ->
            (p.getDate().isEqual(dateFrom) || p.getDate().isAfter(dateFrom))
                && p.getDate().isBefore(dateTo))
        .collect(toList());
  }

  private List<Backlog> getBacklog(final String logisticCenterId,
                                   final ZonedDateTime dateFrom,
                                   final ZonedDateTime dateTo) {
    try {
      final String groupingKey = BacklogGrouper.DATE_OUT.getName();

      var backlogBySla = backlogGateway.getCurrentBacklog(
          logisticCenterId,
          List.of("outbound-orders"),
          CAP5_TO_PACK_STATUSES,
          dateFrom.toInstant(),
          dateTo.toInstant(),
          List.of(groupingKey));

      return backlogBySla.stream()
          .map(backlog -> new Backlog(
              ZonedDateTime.parse(backlog.getKeys().get(groupingKey)),
              backlog.getTotal()))
          .collect(toList());
    } catch (RuntimeException ex) {
      log.error("Failed getting backlog\n" + ex.getMessage(), ex);
      metricsService.trackProjectionError(logisticCenterId, FBM_WMS_OUTBOUND, PROJECTION_TYPE, "backlog");
      throw ex;
    }
  }
}
