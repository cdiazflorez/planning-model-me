package com.mercadolibre.planning.model.me.gateways.authorization.dtos;

import java.util.Arrays;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum UserPermission {
  UNKNOWN("UNKNOWN"),
  EDIT_INBOUND_PLANNING("edit-inbound-planning"),
  OUTBOUND_FORECAST("OUTBOUND_FORECAST"),
  OUTBOUND_PROJECTION("OUTBOUND_PROJECTION"),
  OUTBOUND_SIMULATION("OUTBOUND_SIMULATION");

  private static final UserPermission[] VALUES = values();

  private final String alias;

  public static UserPermission from(final String value) {
    return Arrays.stream(VALUES)
        .filter(permission -> value.equalsIgnoreCase(permission.alias))
        .findFirst()
        .orElse(UNKNOWN);
  }
}
