package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

public enum EntityType {
    ORDER_UNITS,
    PRODUCTIVITY,
    HEADCOUNT,
    BACKLOG,
    ORDER_DISTRIBUTION;

    public String getName() {
        return name().toLowerCase();
    }
}
