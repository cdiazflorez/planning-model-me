package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.util.List;
import lombok.Value;

@Value
public class SaveSimulationsRequest {

    Workflow workflow;

    String warehouseId;

    List<Simulation> simulations;

    long userId;
}
