package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;

public enum ForecastColumnName implements ForecastColumn {
  WEEK,
  MONO_ORDER_DISTRIBUTION,
  MULTI_ORDER_DISTRIBUTION,
  MULTI_BATCH_DISTRIBUTION,
  PROCESSING_DISTRIBUTION,
  HEADCOUNT_DISTRIBUTION,
  POLYVALENT_PRODUCTIVITY,
  HEADCOUNT_PRODUCTIVITY,
  PLANNING_DISTRIBUTION,
  CARRIER_ID,
  SERVICE_ID,
  CANALIZATION,
  BACKLOG_LIMITS,
  OUTBOUND_PICKING_PRODUCTIVITY,
  OUTBOUND_BATCH_SORTER_PRODUCTIVITY,
  OUTBOUND_WALL_IN_PRODUCTIVITY,
  OUTBOUND_PACKING_PRODUCTIVITY,
  OUTBOUND_PACKING_WALL_PRODUCTIVITY;

  public String getName() {
    return name().toLowerCase();
  }
}
