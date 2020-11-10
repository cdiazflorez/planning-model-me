package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.util.List;

@Value
public class SimulationEntity {
    private EntityType type;
    private List<QuantityByDate> values;
}
