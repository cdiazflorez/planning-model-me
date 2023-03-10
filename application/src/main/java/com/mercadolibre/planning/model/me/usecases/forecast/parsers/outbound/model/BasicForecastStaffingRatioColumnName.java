package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BasicForecastStaffingRatioColumnName implements ForecastStaffingColumnName {
  TOT_MONO(6, ProcessPath.TOT_MONO),
  TOT_MULTI_BATCH(7, ProcessPath.TOT_MULTI_BATCH),
  TOT_MULTI_ORDER(8, ProcessPath.TOT_MULTI_ORDER),
  NON_TOT_MONO(9, ProcessPath.NON_TOT_MONO),
  NON_TOT_MULTI_ORDER(10, ProcessPath.NON_TOT_MULTI_ORDER);
  private final int columnIndex;

  private final ProcessPath processPath;
}
