package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class SaveSimulation implements UseCase<GetProjectionInputDto, PlanningView> {
  final SaveSimulationInbound saveSimulationInbound;
  final SaveSimulationOutbound saveSimulationOutbound;

  @Override
  public PlanningView execute(GetProjectionInputDto input) {
    if (input.getWorkflow().equals(Workflow.FBM_WMS_INBOUND)) {
      return saveSimulationInbound.execute(input);
    } else if (input.getWorkflow().equals(Workflow.FBM_WMS_OUTBOUND)) {
      return saveSimulationOutbound.execute(input);
    }
    return null;
  }
}
