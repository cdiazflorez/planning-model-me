package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SimulationRequest {
    private Workflow workflow;
    private String warehouseId;
    private List<Simulation> simulations;
}
