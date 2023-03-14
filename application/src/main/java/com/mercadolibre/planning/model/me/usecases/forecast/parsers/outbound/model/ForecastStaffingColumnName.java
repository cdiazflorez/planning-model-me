package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.enums.ProcessPath;

/**
 * Interface to mark object as a ForecastStaffingProductivityColumnName.
 */
public interface ForecastStaffingColumnName {

  int getColumnIndex();

  ProcessPath getProcessPath();
}
