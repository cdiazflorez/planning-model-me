package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

import java.util.List;

public interface PlanningModelGateway {

    List<Entity> getEntities(final EntityRequest request);

    void postForecast(final Workflow workflow, final Forecast forecastDto);
}
