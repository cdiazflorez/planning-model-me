package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model;

public enum ForecastColumnName {
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
    CANALIZATION;

    public String getName() {
        return name().toLowerCase();
    }
}
