package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AreaDistribution {
    private String areaId;
    private long quantity;
}
