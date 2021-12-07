package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

public enum Process {
    RECEIVING,
    CHECK_IN,
    PUT_AWAY,
    STAGE_IN;

    public String getName() {
        return name().toLowerCase();
    }
}
