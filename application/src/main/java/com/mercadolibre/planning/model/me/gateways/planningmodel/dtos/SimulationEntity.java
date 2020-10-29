package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SimulationEntity {
    private EntityType type;
    private List<QuantityByDate> values;
}
