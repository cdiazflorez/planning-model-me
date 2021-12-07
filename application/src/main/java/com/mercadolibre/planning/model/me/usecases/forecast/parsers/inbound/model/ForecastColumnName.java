package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;

public enum ForecastColumnName implements ForecastColumn {
    WAREHOUSE_ID,
    WEEK,
    PROCESSING_DISTRIBUTION;

    public String getName() {
        return name().toLowerCase();
    }
}
