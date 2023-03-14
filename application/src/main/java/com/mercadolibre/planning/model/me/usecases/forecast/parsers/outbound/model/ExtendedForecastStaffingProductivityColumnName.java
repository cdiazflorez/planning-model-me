package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExtendedForecastStaffingProductivityColumnName implements ForecastStaffingColumnName {
  TOT_MONO(1, ProcessPath.TOT_MONO),
  TOT_MULTI_BATCH(2, ProcessPath.TOT_MULTI_BATCH),
  TOT_MULTI_ORDER(3, ProcessPath.TOT_MULTI_ORDER),
  NON_TOT_MONO(4, ProcessPath.NON_TOT_MONO),
  NON_TOT_MULTI_ORDER(5, ProcessPath.NON_TOT_MULTI_ORDER),
  NON_TOT_MULTI_BATCH(6, ProcessPath.NON_TOT_MULTI_BATCH);

  private final int columnIndex;

  private final ProcessPath processPath;
}
