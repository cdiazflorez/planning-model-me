package com.mercadolibre.planning.model.me.entities.workflows;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Area {
  PW;

  @JsonValue
  public String getName() {
    return this.toString().toLowerCase();
  }

  @JsonCreator
  public static Area from(final String value) {
    return valueOf(value.toUpperCase());
  }
}
