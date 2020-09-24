package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetProjectionInputDto {
    private Workflow workflow;
    private String warehouseId;
}
