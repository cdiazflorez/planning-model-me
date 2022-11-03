package com.mercadolibre.planning.model.me.entities.workflows;

import java.util.Locale;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BacklogWorkflow {
  OUTBOUND_ORDERS("OUTBOUND-ORDERS"),
  INBOUND("inbound"),
  INBOUND_TRANSFER("INBOUND-TRANSFER");

  private final String name;

  public static Optional<BacklogWorkflow> from(final String value) {
    return Optional.of(BacklogWorkflow.valueOf(value.toUpperCase(Locale.ENGLISH)));
  }
}
