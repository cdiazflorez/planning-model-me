package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

public enum Source {
    FORECAST,
    SIMULATION;

    public static Source from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }
}
