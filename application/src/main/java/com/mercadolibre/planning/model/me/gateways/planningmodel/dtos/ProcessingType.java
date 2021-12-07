package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Getter;

@Getter
public enum ProcessingType {
    ACTIVE_WORKERS,
    ACTIVE_WORKERS_NS,
    PERFORMED_PROCESSING,
    REMAINING_PROCESSING,
    WORKERS,
    WORKERS_NS,
    THROUGHPUT,
    MAX_CAPACITY;

    public static ProcessingType from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }
}
