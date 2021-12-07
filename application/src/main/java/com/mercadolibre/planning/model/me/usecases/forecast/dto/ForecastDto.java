package com.mercadolibre.planning.model.me.usecases.forecast.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

@Value
public class ForecastDto {
    private Workflow workflow;
    private Forecast forecast;
}
