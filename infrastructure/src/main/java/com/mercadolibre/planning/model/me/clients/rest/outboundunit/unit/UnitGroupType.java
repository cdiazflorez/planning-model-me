package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.exception.EnumNotFoundException;

public enum UnitGroupType {

    ORDER, TRANSFER, WITHDRAWAL;

    @JsonCreator
    public static UnitGroupType from(final String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EnumNotFoundException(value, e);
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
