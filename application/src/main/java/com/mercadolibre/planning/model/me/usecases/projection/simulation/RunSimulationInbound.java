package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionInbound;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Named
public class RunSimulationInbound extends GetProjectionInbound {

    protected RunSimulationInbound(final PlanningModelGateway planningModelGateway,
                                   final LogisticCenterGateway logisticCenterGateway,
                                   final GetWaveSuggestion getWaveSuggestion,
                                   final GetEntities getEntities,
                                   final GetProjectionSummary getProjectionSummary,
                                   final GetBacklogByDateInbound getBacklogByDateInbound) {

        super(planningModelGateway, logisticCenterGateway, getWaveSuggestion,
                getEntities, getProjectionSummary, getBacklogByDateInbound);
    }

    @Override
    protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                   final ZonedDateTime dateFrom,
                                                   final ZonedDateTime dateTo,
                                                   final List<Backlog> backlogs,
                                                   final String timeZone) {
        return planningModelGateway.runSimulation(SimulationRequest.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processName(PROCESS_NAMES_INBOUND)
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