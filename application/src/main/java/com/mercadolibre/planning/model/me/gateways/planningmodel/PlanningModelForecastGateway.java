package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

public interface PlanningModelForecastGateway {
    ForecastResponse postForecast(final Workflow workflow, final Forecast forecastDto);

}
