package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import java.util.List;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class WriteSimulation {

    private final ProjectionGateway projectionGateway;

    public void saveSimulations(final Workflow workflow,
                                final String warehouseId,
                                final List<Simulation> simulations,
                                final Long callerId) {


        projectionGateway.deferralSaveSimulation(
                new SaveSimulationsRequest(workflow, warehouseId, simulations, callerId));

    }
}
