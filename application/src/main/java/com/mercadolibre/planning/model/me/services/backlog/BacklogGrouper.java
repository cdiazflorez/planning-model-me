package com.mercadolibre.planning.model.me.services.backlog;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BacklogGrouper {
    DATE_IN,
    DATE_OUT,
    PROCESS,
    AREA;

    @JsonValue
    public String getName() {
        return this.toString().toLowerCase();
    }
}
