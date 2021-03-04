package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuggestedWave {

    private int quantity;

    private Cardinality waveCardinality;

}
