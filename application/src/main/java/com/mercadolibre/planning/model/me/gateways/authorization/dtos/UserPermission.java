package com.mercadolibre.planning.model.me.gateways.authorization.dtos;

import java.util.Arrays;

public enum UserPermission {
    UNKNOWN,
    OUTBOUND_FORECAST,
    OUTBOUND_PROJECTION,
    OUTBOUND_SIMULATION;


    public static UserPermission from(final String value) {
        return Arrays.stream(values())
                .filter(permission -> value.equalsIgnoreCase(permission.name()))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
