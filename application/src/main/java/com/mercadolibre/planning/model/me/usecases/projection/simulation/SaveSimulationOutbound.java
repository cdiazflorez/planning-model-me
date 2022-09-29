package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionOutbound;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectionWorkflow;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;

/**
 * Implementation of GetProjectionOutbound that invokes save simulation on planning model gateway.
 */
@Named
public class SaveSimulationOutbound extends GetProjectionOutbound {

  private final FeatureSwitches featureSwitches;

  private final CalculateProjectionService calculateProjection;

  private final ProjectionGateway projectionGateway;

  protected SaveSimulationOutbound(final PlanningModelGateway planningModelGateway,
                                   final LogisticCenterGateway logisticCenterGateway,
                                   final GetWaveSuggestion getWaveSuggestion,
                                   final GetEntities getEntities,
                                   final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                   final BacklogApiGateway backlogGateway,
                                   final GetSales getSales,
                                   final FeatureSwitches featureSwitches,
                                   final CalculateProjectionService calculateProjection,
                                   final ProjectionGateway projectionGateway,
                                   final RatioService ratioService) {

    super(planningModelGateway, logisticCenterGateway, getWaveSuggestion, getEntities, getSimpleDeferralProjection, backlogGateway,
        getSales, ratioService);
    this.featureSwitches = featureSwitches;
    this.calculateProjection = calculateProjection;
    this.projectionGateway = projectionGateway;
  }

  @Override
  protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs,
                                                 final LogisticCenterConfiguration config) {

    if (featureSwitches.isProjectionLibEnabled(input.getWarehouseId())) {
      projectionGateway.deferralSaveSimulation(
          new SaveSimulationsRequest(
              input.getWorkflow(),
              input.getWarehouseId(),
              input.getSimulations(),
              input.getUserId()
          )
      );

      // TODO: call GetSlaProjectionOutbound
      final var currentBacklog = getCurrentBacklog(input, dateFrom, dateTo);
      final var plannedBacklog = getExpectedBacklog(input.getWarehouseId(), input.getWorkflow(), dateFrom, dateTo);
      final var processingTimes = getSlas(input, dateFrom, dateTo, currentBacklog, plannedBacklog, config.getTimeZone().getID());
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
          getThroughputByProcess(input, dateFrom, dateTo, Collections.emptyList()),
          currentBacklog,
          plannedBacklog,
          processingTimes,
          packingRatio
      );
    } else {
      return planningModelGateway.saveSimulation(SimulationRequest.builder()
          .warehouseId(input.getWarehouseId())
          .workflow(input.getWorkflow())
          .processName(ProjectionWorkflow.getProcesses(FBM_WMS_OUTBOUND))
          .dateFrom(dateFrom)
          .dateTo(dateTo)
          .backlog(backlogs.stream()
              .map(backlog -> new QuantityByDate(
                  backlog.getDate(),
                  backlog.getQuantity()))
              .collect(toList()))
          .simulations(input.getSimulations())
          .userId(input.getUserId())
          .applyDeviation(true)
          .timeZone(config.getTimeZone().getID())
          .build());
    }
  }
}
