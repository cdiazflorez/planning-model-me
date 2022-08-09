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
    POLYVALENT_PICKING,
    POLYVALENT_BATCH_SORTER,
    POLYVALENT_WALL_IN,
    POLYVALENT_PACKING,
    POLYVALENT_PACKING_WALL;

    public String getName() {
        return name().toLowerCase();
    }
}
