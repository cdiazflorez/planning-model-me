package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.util.List;

@Value
public class HeadcountProductivity {
    private String processName;
    private String productivityMetricUnit;
    private int abilityLevel;
    private List<HeadcountProductivityData> data;
}
