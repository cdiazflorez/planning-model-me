package com.mercadolibre.planning.model.me.entities.workflows;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Optional;

public enum Step {
  CANCELLED,
  CHECK_IN,
  DISPATCHED,
  DOCUMENTED,
  FINISHED,
  GROUPING,
  OUT,
  PACKED,
  PACKING,
  PENDING,
  PICK,
  PICKED,
  PUT_AWAY,
  PUTAWAY_PS,
  RETURNED,
  RETURNED_DAMAGE_REJECTED,
  SCHEDULED,
  SORTED,
  SPLIT,
  TEMP_UNAVAILABLE,
  TO_DISPATCH,
  TO_DOCUMENT,
  TO_EXPEDITION,
  TO_GROUP,
  TO_OUT,
  TO_PACK,
  TO_PICK,
  TO_RESTOCK,
  TO_ROUTE,
  TO_SORT,
  UNAVAILABLE;

  @JsonCreator
  public static Optional<Step> from(final String value) {
    return Optional.of(Step.valueOf(value.toUpperCase(Locale.ENGLISH)));
  }

  @JsonValue
  public String getName() {
    return this.toString();
  }
}
