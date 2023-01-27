package com.mercadolibre.planning.model.me.entities.workflows;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BacklogWorkflow {
  OUTBOUND_ORDERS("OUTBOUND-ORDERS", "outbound_orders"),
  INBOUND("inbound", "inbound"),
  INBOUND_TRANSFER("INBOUND-TRANSFER", "inbound_transfer");

  private final String name;

  private final String jsonProperty;

  public static Optional<BacklogWorkflow> from(final String name) {
    return Arrays.stream(BacklogWorkflow.values())
            .filter(b -> name.equalsIgnoreCase(b.getName()))
            .findFirst();
  }
}
