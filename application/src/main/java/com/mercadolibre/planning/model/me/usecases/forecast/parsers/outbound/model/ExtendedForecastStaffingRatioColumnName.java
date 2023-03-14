package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExtendedForecastStaffingRatioColumnName implements ForecastStaffingColumnName {
  TOT_MONO(7, ProcessPath.TOT_MONO),
  TOT_MULTI_BATCH(8, ProcessPath.TOT_MULTI_BATCH),
  TOT_MULTI_ORDER(9, ProcessPath.TOT_MULTI_ORDER),
  NON_TOT_MONO(10, ProcessPath.NON_TOT_MONO),
  NON_TOT_MULTI_ORDER(11, ProcessPath.NON_TOT_MULTI_ORDER),
  NON_TOT_MULTI_BATCH(12, ProcessPath.NON_TOT_MULTI_BATCH);

  private final int columnIndex;

  private final ProcessPath processPath;
}
