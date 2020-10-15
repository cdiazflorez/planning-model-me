package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

@Value
public class GetBacklogInputDto {
    private Workflow workflow;
    private String warehouseId;
}
