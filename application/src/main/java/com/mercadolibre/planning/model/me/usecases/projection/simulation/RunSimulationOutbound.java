package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Named;

/**
 * Implementation of GetProjectionOutbound that invokes run simulation on planning model gateway.
 */
@Named
public class RunSimulationOutbound extends GetProjectionOutbound {

  private final FeatureSwitches featureSwitches;

  private final CalculateProjectionService calculateProjection;

  private final RatioService ratioService;

  protected RunSimulationOutbound(final PlanningModelGateway planningModelGateway,
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
        getSales);
    this.featureSwitches = featureSwitches;
    this.calculateProjection = calculateProjection;
    this.ratioService = ratioService;
  }

  @Override
  protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs,
                                                 final String timeZone) {

    if (featureSwitches.isProjectionLibEnabled(input.getWarehouseId())) {
      final var thpProjected = getThroughputByProcess(input, dateFrom, dateTo, emptyList());
      final var tphSimulated = getThroughputByProcess(input, dateFrom, dateTo, input.getSimulations());

      final var currentBacklog = getCurrentBacklog(input, dateFrom, dateTo);
      final var plannedBacklog = getExpectedBacklog(input.getWarehouseId(), input.getWorkflow(), dateFrom, dateTo);
      final var defaultSlas = getSlas(input, dateFrom, dateTo, currentBacklog, plannedBacklog, timeZone);
      final var packingRatio = ratioService.getPackingRatio(
          input.getWarehouseId(),
          input.getRequestDate(),
          dateTo.toInstant().plus(2, HOURS),
          dateFrom.toInstant(),
          dateTo.toInstant()
      );

      final var projections = calculateProjection.execute(
          input.getRequestDate(),
          Instant.from(dateTo),
          input.getWorkflow(),
          thpProjected,
          currentBacklog,
          plannedBacklog,
          defaultSlas,
          packingRatio
      );

      final var simulations = calculateProjection.execute(
          input.getRequestDate(),
          Instant.from(dateTo),
          input.getWorkflow(),
          tphSimulated,
          currentBacklog,
          plannedBacklog,
          defaultSlas,
          packingRatio
      );

      copyProjectedEndDateFromSimulationsIntoProjections(projections, simulations);
      return projections;
    } else {
      return planningModelGateway.runSimulation(SimulationRequest.builder()
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
          .timeZone(timeZone)
          .build());
    }
  }

  private void copyProjectedEndDateFromSimulationsIntoProjections(final List<ProjectionResult> projections,
                                                                  final List<ProjectionResult> simulations) {

    final var simulatedEndDates = simulations.stream()
        .collect(Collectors.toMap(
            ProjectionResult::getDate,
            projection -> Optional.ofNullable(projection.getProjectedEndDate())
        ));

    projections.forEach(item -> item.setSimulatedEndDate(
            simulatedEndDates.getOrDefault(item.getDate(), empty()).orElse(null)
        )
    );
  }
}
