package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.util.Map;
import java.util.function.Supplier;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetSlaProjection implements UseCase<GetProjectionInputDto, PlanningView> {

  final GetSlaProjectionInbound getSlaProjectionInbound;
  final GetSlaProjectionOutbound getSlaProjectionOutbound;

  @Override
  public PlanningView execute(GetProjectionInputDto input) {
    Map<Workflow, Supplier<PlanningView>> projectionOption = Map.of(
        Workflow.FBM_WMS_INBOUND, () -> getSlaProjectionInbound.execute(input),
        Workflow.FBM_WMS_OUTBOUND, () -> getSlaProjectionOutbound.execute(input));

    return projectionOption.get(input.getWorkflow()).get();
  }
}

