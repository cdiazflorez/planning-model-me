package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.Map;
import java.util.function.Supplier;

@Named
@AllArgsConstructor
public class GetSlaProjection implements UseCase<GetProjectionInputDto, Projection> {

    final GetSlaProjectionInbound getSlaProjectionInbound;
    final GetSlaProjectionOutbound getSlaProjectionOutbound;

    @Override
    public Projection execute(GetProjectionInputDto input) {
        Map<Workflow, Supplier<Projection>> projectionOption = Map.of(
                Workflow.FBM_WMS_INBOUND, () -> getSlaProjectionInbound.execute(input),
                Workflow.FBM_WMS_OUTBOUND, () -> getSlaProjectionOutbound.execute(input));

        return projectionOption.get(input.getWorkflow()).get();
    }
}
