package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

public interface BacklogGateway {

    boolean supportsWorkflow(final Workflow workflow);

    ProcessBacklog getUnitBacklog(final UnitProcessBacklogInput input);

}
