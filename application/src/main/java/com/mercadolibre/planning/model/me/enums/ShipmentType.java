package com.mercadolibre.planning.model.me.enums;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public enum ShipmentType {
  SPD,
  FTL,
  PRIVATE,
  COLLECT,
  TRANSFER_SHIPMENT;

  private static final Map<String, ShipmentType> LOOKUP = Arrays.stream(values()).collect(
      toMap(ShipmentType::name, Function.identity())
  );

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault()).replace("_", "-");
  }

  @JsonCreator
  public static ShipmentType from(final String value) {
    return LOOKUP.get(value.toUpperCase(Locale.getDefault()));
  }

}
