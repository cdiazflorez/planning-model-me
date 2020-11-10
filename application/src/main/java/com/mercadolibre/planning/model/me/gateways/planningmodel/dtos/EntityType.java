package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

public enum EntityType {
    ORDER_UNITS,
    PRODUCTIVITY,
    HEADCOUNT,
    THROUGHPUT,
    BACKLOG,
    ORDER_DISTRIBUTION;

    public static EntityType from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }
}
