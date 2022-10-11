package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.Value;

import java.util.List;

@Value
public class HeadcountProductivity {
    ProcessPath processPath;
    String processName;
    String productivityMetricUnit;
    int abilityLevel;
    List<HeadcountProductivityData> data;
}
