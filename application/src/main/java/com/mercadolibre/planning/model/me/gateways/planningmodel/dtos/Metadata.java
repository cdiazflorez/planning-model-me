package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Metadata {
    private String key;
    private String value;
}
