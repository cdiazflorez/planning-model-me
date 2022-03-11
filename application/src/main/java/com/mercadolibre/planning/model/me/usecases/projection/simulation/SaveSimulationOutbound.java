package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionOutbound;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import java.time.ZonedDateTime;
import java.util.List;
import javax.inject.Named;

/** Implementation of GetProjectionOutbound that invokes save simulation on planning model gateway. */
@Named
public class SaveSimulationOutbound extends GetProjectionOutbound {


  protected SaveSimulationOutbound(final PlanningModelGateway planningModelGateway,
                                   final LogisticCenterGateway logisticCenterGateway,
                                   final GetWaveSuggestion getWaveSuggestion,
                                   final GetEntities getEntities,
                                   final GetProjectionSummary getProjectionSummary,
                                   final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                   final BacklogApiGateway backlogGateway,
                                   final FeatureSwitches featureSwitches) {

    super(planningModelGateway, logisticCenterGateway, getWaveSuggestion, getEntities, getProjectionSummary,
        getSimpleDeferralProjection, backlogGateway, featureSwitches);
  }

  @Override
  protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs,
                                                 final String timeZone) {

    return planningModelGateway.saveSimulation(SimulationRequest.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processName(getProjectionProcesses(input.getWarehouseId()))
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
