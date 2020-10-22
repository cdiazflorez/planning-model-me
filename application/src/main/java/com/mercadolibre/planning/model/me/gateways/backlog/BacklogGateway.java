package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

import java.util.List;

public interface BacklogGateway {

    List<Backlog> getBacklog(final String warehouseId);

    boolean supportsWorkflow(final Workflow workflow);
}