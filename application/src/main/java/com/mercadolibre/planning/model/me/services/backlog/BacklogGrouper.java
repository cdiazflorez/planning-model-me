package com.mercadolibre.planning.model.me.services.backlog;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BacklogGrouper {
    DATE_OUT,
    PROCESS;

    @JsonValue
    public String getName() {
        return this.toString().toLowerCase();
    }
}
