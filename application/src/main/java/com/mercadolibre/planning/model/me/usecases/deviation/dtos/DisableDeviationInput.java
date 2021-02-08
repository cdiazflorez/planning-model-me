package com.mercadolibre.planning.model.me.usecases.deviation.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
public class DisableDeviationInput {

    private String warehouseId;

    private Workflow workflow;

}
