package com.mercadolibre.planning.model.me.entities.workflows;

import java.util.Locale;
import java.util.Optional;

public enum BacklogWorkflow {
  OUTBOUND_ORDERS,
  INBOUND;

  public static Optional<BacklogWorkflow> from(final String value) {
    return Optional.of(BacklogWorkflow.valueOf(value.toUpperCase(Locale.ENGLISH)));
  }

  public String getName() {
    return this.toString().toLowerCase();
  }
}
