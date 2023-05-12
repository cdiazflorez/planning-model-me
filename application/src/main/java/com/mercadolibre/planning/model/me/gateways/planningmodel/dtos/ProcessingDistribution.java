package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import java.util.List;
import lombok.Value;

@Value
public class ProcessingDistribution {
    String type;
    String quantityMetricUnit;
    String processName;
    ProcessPath processPath;
    List<ProcessingDistributionData> data;
}
