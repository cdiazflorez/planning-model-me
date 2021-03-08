package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum ForecastProcessType {
    PERFORMED_PROCESSING(0, UNITS),
    REMAINING_PROCESSING(0, MINUTES),
    WORKERS(1, MetricUnit.WORKERS),
    ACTIVE_WORKERS(2, MetricUnit.WORKERS),
    HEADCOUNT_PRODUCTIVITY(3, UNITS_PER_HOUR),
    MAX_CAPACITY(0, UNITS_PER_HOUR);

    private final int columnOrder;
    private final MetricUnit metricUnit;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    private static final Map<String, ForecastProcessType> LOOKUP = Arrays.stream(values()).collect(
            toMap(ForecastProcessType::toString, Function.identity())
    );

    @JsonCreator
    public static ForecastProcessType from(String status) {
        return LOOKUP.get(status);
    }
}
