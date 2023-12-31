package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchUnitFilterRequestStringValue {
    WAREHOUSE_ID,
    ID,
    GROUP_TYPE,
    STATUS,
    LIMIT,
    CARDINALITY,
    CARRIER_NAME,
    CARRIER_ID,
    SERVICE_NAME,
    SERVICE_ID,
    DATE_CREATED_FROM,
    DATE_CREATED_TO,
    ETD_FROM,
    ETD_TO,
    VOLUME_FROM,
    VOLUME_TO;

    @JsonCreator
    public static SearchUnitFilterRequestStringValue from(final String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
