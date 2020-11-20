package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Named
public class SaveSimulation extends GetProjection {

    protected SaveSimulation(final PlanningModelGateway planningModelGateway,
                             final LogisticCenterGateway logisticCenterGateway,
                             final GetBacklog getBacklog,
                             final GetSales getSales) {
        super(planningModelGateway, logisticCenterGateway, getBacklog, getSales);
    }

    @Override
    protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                   final ZonedDateTime dateFrom,
                                                   final ZonedDateTime dateTo,
                                                   final List<Backlog> backlogs) {

        return planningModelGateway.saveSimulation(SimulationRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(input.getWorkflow())
                        .processName(PROJECTION_PROCESS_NAMES)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .backlog(backlogs.stream()
                                .map(backlog -> new QuantityByDate(
                                        backlog.getDate(),
                                        backlog.getQuantity()))
                                .collect(toList()))
                        .simulations(input.getSimulations())
                        .build());
    }

}
