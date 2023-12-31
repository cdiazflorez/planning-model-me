package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;

public enum ForecastColumnName implements ForecastColumn {
    WAREHOUSE_ID,
    WEEK,
    PROCESSING_DISTRIBUTION,
    HEADCOUNT_PRODUCTIVITY,
    POLYVALENT_PRODUCTIVITY,
    BACKLOG_LIMITS,
    INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES,
    INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES,
    INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;

    public String getName() {
        return name().toLowerCase();
    }
}
