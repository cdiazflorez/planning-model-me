package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

@Named
@AllArgsConstructor
public class SaveSimulation implements UseCase<GetProjectionInputDto, Projection> {
    final SaveSimulationInbound saveSimulationInbound;
    final SaveSimulationOutbound saveSimulationOutbound;

    @Override
    public Projection execute(GetProjectionInputDto input) {
        if (input.getWorkflow().equals(Workflow.FBM_WMS_INBOUND)) {
            return saveSimulationInbound.execute(input);
        } else if (input.getWorkflow().equals(Workflow.FBM_WMS_OUTBOUND)) {
            return saveSimulationOutbound.execute(input);
        }
        return null;
    }
}
