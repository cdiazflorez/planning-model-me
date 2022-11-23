package com.mercadolibre.planning.model.me.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.ProcessPathNotFoundException;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessPath {
  GLOBAL,
  TOT_MONO,
  TOT_MULTI_BATCH,
  TOT_MULTI_ORDER,
  NON_TOT_MONO,
  NON_TOT_MULTI_ORDER,
  NON_TOT_MULTI_BATCH,
  CONV_MULTI_BATCH,
  CONV_MONO,
  BULKY,
  PP_DEFAULT_MONO,
  PP_DEFAULT_MULTI,
  SIOC,
  AMBIENT,
  REFRIGERATED;

  @JsonCreator
  public static ProcessPath from(final String value) {
      try {
          return valueOf(value.toUpperCase(Locale.getDefault()));
      } catch (IllegalArgumentException e) {
          throw new ProcessPathNotFoundException(value, e);
      }
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault());
  }
}
