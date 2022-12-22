package com.mercadolibre.planning.model.me.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ShipmentType {
  SPD,
  FTL,
  PRIVATE,
  COLLECT,
  TRANSFER_SHIPMENT;

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault()).replace("_", "-");
  }
}
