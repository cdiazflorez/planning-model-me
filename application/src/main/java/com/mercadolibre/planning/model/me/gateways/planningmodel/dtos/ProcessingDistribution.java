package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.util.List;

@Value
public class ProcessingDistribution {
    private String type;
    private String quantityMetricUnit;
    private String processName;
    private List<ProcessingDistributionData> data;
}
