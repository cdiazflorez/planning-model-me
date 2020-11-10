package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConfigurationRequest {
    private String warehouseId;
    private String key;
}
