package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastStaffingRatioColumnName {
  TOT_MONO(9, ProcessPath.TOT_MONO),
  TOT_MULTI_BATCH(10, ProcessPath.TOT_MULTI_BATCH),
  TOT_MULTI_ORDER(11, ProcessPath.TOT_MULTI_ORDER),
  NON_TOT_MONO(12, ProcessPath.NON_TOT_MONO),
  NON_TOT_MULTI_ORDER(13, ProcessPath.NON_TOT_MULTI_ORDER),
  NON_TOT_MULTI_BATCH(14, ProcessPath.NON_TOT_MULTI_BATCH),
  PP_DEFAULT_MONO(15, ProcessPath.PP_DEFAULT_MONO),
  PP_DEFAULT_MULTI(16, ProcessPath.PP_DEFAULT_MULTI);

  private final int columnIndex;

  private final ProcessPath processPath;
}
