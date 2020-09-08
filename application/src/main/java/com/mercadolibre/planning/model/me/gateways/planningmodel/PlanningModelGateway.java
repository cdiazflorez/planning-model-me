package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;

import java.util.List;

public interface PlanningModelGateway {

    List<Entity> getEntities(final EntityRequest request);
}
