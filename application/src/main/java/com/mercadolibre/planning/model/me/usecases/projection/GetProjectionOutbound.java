package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import com.mercadolibre.planning.model.me.utils.ResponseUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements template method for outbound SLAs projections and simulations.
 *
 * <p>
 *   Includes common behaviour for outbound projections as retrieving backlog, mapping projections and building responses.
 * </p>
 */
@Slf4j
public abstract class GetProjectionOutbound extends GetProjection {

  private final GetSimpleDeferralProjection getSimpleDeferralProjection;

  private final BacklogApiGateway backlogGateway;

  private final GetWaveSuggestion getWaveSuggestion;

  protected GetProjectionOutbound(final PlanningModelGateway planningModelGateway,
                                  final LogisticCenterGateway logisticCenterGateway,
                                  final GetWaveSuggestion getWaveSuggestion,
                                  final GetEntities getEntities,
                                  final GetProjectionSummary getProjectionSummary,
                                  final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                  final BacklogApiGateway backlogGateway) {

    super(planningModelGateway, logisticCenterGateway, getEntities, getProjectionSummary);

    this.getSimpleDeferralProjection = getSimpleDeferralProjection;
    this.getWaveSuggestion = getWaveSuggestion;
    this.backlogGateway = backlogGateway;
  }

  @Override
  protected final List<ProjectionResult> decorateProjection(final GetProjectionInputDto input,
                                                            final List<Backlog> backlogsToProject,
                                                            final List<ProjectionResult> projectionsSla) {

    final GetSimpleDeferralProjectionOutput deferralProjectionOutput =
        getSimpleDeferralProjection.execute(
            new GetProjectionInput(
                input.getWarehouseId(),
                input.getWorkflow(),
                input.getDate(),
                backlogsToProject,
                false));

    return setIsDeferred(projectionsSla, deferralProjectionOutput.getProjections());
  }

  @Override
  protected final List<Backlog> getBacklog(final Workflow workflow,
                                           final String warehouseId,
                                           final Instant dateFromToProject,
                                           final Instant dateToToProject,
                                           final ZoneId zoneId,
                                           final Instant requestDate) {

    final String groupingKey = BacklogGrouper.DATE_OUT.getName();

    final var backlogBySla = backlogGateway.getCurrentBacklog(
        warehouseId,
        List.of("outbound-orders"),
        ProjectionWorkflow.getStatuses(FBM_WMS_OUTBOUND),
        dateFromToProject,
        dateToToProject,
        List.of(groupingKey));


    return backlogBySla.stream()
        .map(backlog -> new Backlog(
            ZonedDateTime.parse(backlog.getKeys().get(groupingKey)),
            backlog.getTotal()))
        .collect(toList());
  }

  @Override
  public final List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
                                           final ZoneId zoneId,
                                           final ZonedDateTime dateTo) {
    final boolean hasSimulatedResults = hasSimulatedResults(projectionResult);

    return projectionResult.stream()
        .map(projection -> {
          final ZonedDateTime projectedEndDate = hasSimulatedResults
              ? projection.getSimulatedEndDate()
              : projection.getProjectedEndDate();

          return ChartData.fromProjection(
              convertToTimeZone(zoneId, projection.getDate()),
              projectedEndDate == null
                  ? null : convertToTimeZone(zoneId, projectedEndDate),
              convertToTimeZone(zoneId, dateTo),
              projection.getRemainingQuantity(),
              projection.getProcessingTime(),
              projection.isDeferred(),
              projection.isExpired());
        })
        .collect(toList());
  }

  @Override
  protected final SimpleTable getWaveSuggestionTable(final String warehouseID,
                                                     final Workflow workflow,
                                                     final ZoneId zoneId,
                                                     final ZonedDateTime date) {
    return getWaveSuggestion.execute(GetWaveSuggestionInputDto.builder()
        .zoneId(zoneId)
        .warehouseId(warehouseID)
        .workflow(workflow)
        .date(date)
        .build());
  }

  @Override
  protected final SimpleTable getProjectionSummaryTable(final ZonedDateTime dateFromToShow,
                                                        final ZonedDateTime dateToToShow,
                                                        final GetProjectionInputDto input,
                                                        final List<ProjectionResult> projectionsToShow,
                                                        final List<Backlog> backlogsToShow) {
    return getProjectionSummary.execute(GetProjectionSummaryInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(dateFromToShow)
        .dateTo(dateToToShow)
        .projections(projectionsToShow)
        .backlogs(backlogsToShow)
        .showDeviation(true)
        .build());
  }

  @Override
  protected final List<Tab> createTabs() {
    return ResponseUtils.createOutboundTabs();
  }

  private List<ProjectionResult> setIsDeferred(final List<ProjectionResult> slaProjections,
                                               final List<ProjectionResult> deferralProjections) {

    final Map<Instant, ProjectionResult> deferralProjectionsByDateOut =
        deferralProjections.stream().collect(Collectors.toMap(
            pt -> pt.getDate().toInstant(),
            Function.identity(),
            (pd1, pd2) -> pd2
        ));

    final List<ProjectionResult> newSlaProjections = new ArrayList<>(slaProjections);

    for (ProjectionResult slaProjection : newSlaProjections) {
      final ProjectionResult deferralProjection =
          deferralProjectionsByDateOut.get(slaProjection.getDate().toInstant());
      if (deferralProjection != null) {
        slaProjection.setDeferred(deferralProjection.isDeferred());
      } else {
        log.info("Not found cptProjection [{}] in cptDeferral", slaProjection.getDate()
            .toInstant());
      }
    }

    return newSlaProjections;
  }
}
