package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DynamicForecastStaffingColumnName implements ForecastStaffingColumnName {
  private ProcessPath processPath;

  private int columnIndex;


  @Override
  public int getColumnIndex() {
    return columnIndex;
  }

  @Override
  public ProcessPath getProcessPath() {
    return processPath;
  }
}
