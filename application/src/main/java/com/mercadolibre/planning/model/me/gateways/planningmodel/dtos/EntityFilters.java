package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EntityFilters {
    PROCESSING_TYPE,
    ABILITY_LEVEL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
