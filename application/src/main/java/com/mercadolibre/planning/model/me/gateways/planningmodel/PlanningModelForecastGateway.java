package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PostForecastResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

public interface PlanningModelForecastGateway {
    PostForecastResponse postForecast(final Workflow workflow, final Forecast forecastDto);

}
