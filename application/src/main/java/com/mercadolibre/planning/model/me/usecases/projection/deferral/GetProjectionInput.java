package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

@Value
public class GetProjectionInput {

    private String logisticCenterId;

    private Workflow workflow;
}
