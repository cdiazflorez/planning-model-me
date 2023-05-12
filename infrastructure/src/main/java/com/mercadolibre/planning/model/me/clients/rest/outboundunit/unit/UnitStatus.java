package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;

public enum UnitStatus {

    PENDING,
    TO_ROUTE,
    TO_PICK,
    PICKED,
    TO_GROUP,
    GROUPING,
    GROUPED,
    TO_PACK,
    PACKED,
    OUT,
    CANCELLED,
    TEMP_UNAVAILABLE,
    UNAVAILABLE,
    UNKNOWN;

    private static final Map<String, UnitStatus> LOOKUP =
            stream(values()).collect(toMap(UnitStatus::toString, Function.identity()));

    @JsonCreator
    public static UnitStatus from(final String value) {
        return LOOKUP.getOrDefault(value.toUpperCase(), UNKNOWN);
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
