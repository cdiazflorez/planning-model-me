package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PolyvalentProductivity {
    private String processName;
    private String productivityMetricUnit;
    private double productivity;
    private int abilityLevel;
}
