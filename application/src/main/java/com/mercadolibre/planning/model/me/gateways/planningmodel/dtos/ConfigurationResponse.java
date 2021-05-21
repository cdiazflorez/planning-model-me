package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationResponse {
    private long value;
    private MetricUnit metricUnit;
}
