package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.util.List;
import lombok.Value;

@Value
public class SimulationEntity {
    private MagnitudeType type;
    private List<QuantityByDate> values;
}
