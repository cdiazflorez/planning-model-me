package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Getter;

@Getter
public enum ProcessingType {
    ACTIVE_WORKERS,
    PERFORMED_PROCESSING,
    REMAINING_PROCESSING,
    WORKERS,
    MAX_CAPACITY;

    public static ProcessingType from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }
}
