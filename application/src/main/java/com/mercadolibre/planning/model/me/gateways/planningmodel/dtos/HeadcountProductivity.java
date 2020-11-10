package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HeadcountProductivity {
    private String processName;
    private String productivityMetricUnit;
    private int abilityLevel;
    private List<HeadcountProductivityData> data;
}
