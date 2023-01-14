package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public enum SheetVersion {


  CURRENT_VERSION("1.0"),
  NON_SYSTEMIC_VERSION_OB("2.0");

  private String version;

  private static final Map<String, SheetVersion> LOOKUP =
      Arrays.stream(values()).collect(toMap(SheetVersion::getVersion,  Function.identity()));

  public static SheetVersion from(final String version) {
    if (LOOKUP.containsKey(version)) {
      return LOOKUP.get(version);
    }
    log.info("[Version: {}] No encontrada, se toma la version por defecto", version);
    return CURRENT_VERSION;

  }

}
