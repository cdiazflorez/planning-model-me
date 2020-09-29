package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

public enum MetricUnit {
    UNITS,
    UNITS_PER_HOUR,
    PERCENTAGE,
    MINUTES,
    WORKERS;

    public static MetricUnit from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }
}
