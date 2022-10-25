package com.mercadolibre.planning.model.me.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ProcessPath {
  GLOBAL,
  TOT_MONO,
  TOT_MULTI_BATCH,
  TOT_MULTI_ORDER,
  NON_TOT_MONO,
  NON_TOT_MULTI_ORDER,
  NON_TOT_MULTI_BATCH,
  PP_DEFAULT_MONO,
  PP_DEFAULT_MULTI,
  SIOC,
  AMBIENT,
  REFRIGERATED;

  @JsonCreator
  public static ProcessPath from(final String value) {
    return valueOf(value.toUpperCase(Locale.getDefault()));
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault());
  }
}
