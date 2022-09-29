package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType.CPT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;

/**
 * Implements method for outbound SLAs projection.
 *
 * <p>
 * Its responsibility is to call the run simulation method on planning gateway.
 * </p>
 */
@Named
public class GetSlaProjectionOutbound extends GetProjectionOutbound {

  private final FeatureSwitches featureSwitches;

  private final CalculateProjectionService calculateProjection;

  protected GetSlaProjectionOutbound(final PlanningModelGateway planningModelGateway,
                                     final LogisticCenterGateway logisticCenterGateway,
                                     final GetWaveSuggestion getWaveSuggestion,
                                     final GetEntities getEntities,
                                     final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                     final BacklogApiGateway backlogGateway,
                                     final GetSales getSales,
                                     final FeatureSwitches featureSwitches,
                                     final CalculateProjectionService calculateProjection,
                                     final RatioService ratioService) {

    super(planningModelGateway, logisticCenterGateway, getWaveSuggestion, getEntities, getSimpleDeferralProjection, backlogGateway,
        getSales, ratioService);
    this.featureSwitches = featureSwitches;
    this.calculateProjection = calculateProjection;
  }


  @Override
  protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs,
                                                 final LogisticCenterConfiguration config) {

    if (featureSwitches.isProjectionLibEnabled(input.getWarehouseId())) {

      final var currentBacklog = getCurrentBacklog(input, dateFrom, dateTo);
      final var plannedBacklog = getExpectedBacklog(input.getWarehouseId(), input.getWorkflow(), dateFrom, dateTo);
      final var defaultSlas = getSlas(input, dateFrom, dateTo, currentBacklog, plannedBacklog, config.getTimeZone().getID());
      final var packingRatio = getPackingRatio(
          input.getWarehouseId(),
          config.isPutToWall(),
          input.getRequestDate(),
          dateTo.toInstant().plus(2, HOURS),
          dateFrom.toInstant(),
          dateTo.toInstant()
      );

      return calculateProjection.execute(
          input.getRequestDate(),
          Instant.from(dateFrom),
          Instant.from(dateTo),
          input.getWorkflow(),
          getThroughputByProcess(input, dateFrom, dateTo, emptyList()),
          currentBacklog,
          plannedBacklog,
          defaultSlas,
          packingRatio
      )
          .stream()
          .sorted(Comparator.comparing(ProjectionResult::getDate))
          .collect(Collectors.toList());
    } else {
      return planningModelGateway.runProjection(ProjectionRequest.builder()
          .warehouseId(input.getWarehouseId())
          .workflow(input.getWorkflow())
          .processName(ProjectionWorkflow.getProcesses(FBM_WMS_OUTBOUND))
          .type(CPT)
          .dateFrom(dateFrom)
          .dateTo(dateTo)
          .backlog(backlogs)
          .userId(input.getUserId())
          .applyDeviation(true)
          .timeZone(config.getTimeZone().getID())
          .build());
    }
  }
}
