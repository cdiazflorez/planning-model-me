package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetProjectionInputDto {

    private Workflow workflow;

    private String warehouseId;

    private List<Simulation> simulations;

    private long userId;
}
