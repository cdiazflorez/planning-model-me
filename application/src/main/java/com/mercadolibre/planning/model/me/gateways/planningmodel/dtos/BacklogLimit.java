package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;

import lombok.Value;

import java.util.List;

@Value
public class BacklogLimit {
    private ForecastProcessType type;
    private MetricUnit quantityMetricUnit;
    private ForecastProcessName processName;
    private List<BacklogLimitData> data;
}
