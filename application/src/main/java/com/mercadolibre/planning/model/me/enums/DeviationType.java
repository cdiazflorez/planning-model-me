package com.mercadolibre.planning.model.me.enums;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public enum DeviationType {
  UNITS,
  MINUTES;

  private static final Map<String, DeviationType> LOOKUP = Arrays.stream(values()).collect(
      toMap(DeviationType::name, Function.identity())
  );

  @JsonCreator
  public static DeviationType from(final String value) {
    return LOOKUP.get(value.toUpperCase(Locale.getDefault()));
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault());
  }
}
