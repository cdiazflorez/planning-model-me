package com.mercadolibre.planning.model.me.gateways.authorization.dtos;

import java.util.Arrays;

public enum UserPermission {
    UNKNOWN,
    WAVE_READ,
    WAVE_WRITE;

    public static UserPermission from(final String value) {
        return Arrays.stream(values())
                .filter(permission -> value.equalsIgnoreCase(permission.name()))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
