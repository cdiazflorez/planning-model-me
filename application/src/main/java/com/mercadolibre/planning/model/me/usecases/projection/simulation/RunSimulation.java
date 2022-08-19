package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class RunSimulation implements UseCase<GetProjectionInputDto, PlanningView> {
  final RunSimulationInbound runSimulationInbound;
  final RunSimulationOutbound runSimulationOutbound;

  @Override
  public PlanningView execute(GetProjectionInputDto input) {
    if (input.getWorkflow().equals(Workflow.FBM_WMS_INBOUND)) {
      return runSimulationInbound.execute(input);
    } else if (input.getWorkflow().equals(Workflow.FBM_WMS_OUTBOUND)) {
      return runSimulationOutbound.execute(input);
    }
    return null;
  }
}
