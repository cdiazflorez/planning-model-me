package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Data
@Builder
public class SuggestedWave {

    private int quantity;

    private Cardinality waveCardinality;

}
