package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportAggregationsTotals {
    TOTAL_ORDERS;

    @JsonCreator
    public ReportAggregationsTotals from(String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toLowerCase();
    }
}
