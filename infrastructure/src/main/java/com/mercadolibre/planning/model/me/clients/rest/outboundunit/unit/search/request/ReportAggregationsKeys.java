package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportAggregationsKeys {
    GROUP_ESTIMATED_TIME_DEPARTURE;

    @JsonCreator
    public ReportAggregationsKeys from(String value) {
        return this.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toLowerCase();
    }
}
