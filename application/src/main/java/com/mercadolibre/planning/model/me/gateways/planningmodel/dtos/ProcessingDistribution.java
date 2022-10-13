package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.Value;

import java.util.List;

@Value
public class ProcessingDistribution {
    String type;
    String quantityMetricUnit;
    String processName;
    ProcessPath processPath;
    List<ProcessingDistributionData> data;
}
