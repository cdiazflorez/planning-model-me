package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProcessingDistribution {
    private String type;
    private String quantityMetricUnit;
    private String processName;
    private List<ProcessingDistributionData> data;
}
