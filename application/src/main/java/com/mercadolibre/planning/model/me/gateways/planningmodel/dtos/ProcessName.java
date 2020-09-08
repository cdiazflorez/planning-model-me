package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

public enum ProcessName {
    PICKING,
    PACKING;

    public static ProcessName from(final String value) {
        return valueOf(value.toUpperCase());
    }
}
