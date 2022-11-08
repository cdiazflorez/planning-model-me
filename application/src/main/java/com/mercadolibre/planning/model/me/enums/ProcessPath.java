package com.mercadolibre.planning.model.me.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessPath {
  GLOBAL("global"),
  TOT_MONO("totable mono"),
  TOT_MULTI_BATCH("totable multi batch"),
  TOT_MULTI_ORDER("totable multi order"),
  NON_TOT_MONO("non totable mono"),
  NON_TOT_MULTI_ORDER("non tot multi order"),
  NON_TOT_MULTI_BATCH("non tot multi batch"),
  PP_DEFAULT_MONO("pp default mono"),
  PP_DEFAULT_MULTI("pp default multi"),
  SIOC("sioc"),
  AMBIENT("ambient"),
  REFRIGERATED("refrigerated");

  private static final Map<String, ProcessPath> PROCESS_PATH_BY_DESCRIPTION = Arrays.stream(ProcessPath.values())
      .collect(Collectors.toMap(ProcessPath::getDescription, Function.identity()));

  private final String description;

  @JsonCreator
  public static ProcessPath from(final String value) {
    return valueOf(value.toUpperCase(Locale.getDefault()));
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase(Locale.getDefault());
  }

  public static ProcessPath fromDescription(final String description) {
    return PROCESS_PATH_BY_DESCRIPTION.get(description.toLowerCase(Locale.getDefault()));
  }
}
