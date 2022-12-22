package com.mercadolibre.planning.model.me.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum DeviationType {
  UNITS,
  MINUTES;

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault());
  }
}
