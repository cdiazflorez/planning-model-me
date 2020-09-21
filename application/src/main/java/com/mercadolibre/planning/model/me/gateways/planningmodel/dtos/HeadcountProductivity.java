package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HeadcountProductivity {
    private String processName;
    private String productivityMetricUnit;
    private int abilityLevel;
    private List<HeadcountProductivityData> data;
}
