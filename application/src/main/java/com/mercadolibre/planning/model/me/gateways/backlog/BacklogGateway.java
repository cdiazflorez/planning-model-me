package com.mercadolibre.planning.model.me.gateways.backlog;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public interface BacklogGateway {

    boolean supportsWorkflow(final Workflow workflow);

    List<Backlog> getBacklog(final String warehouseId);

    List<ProcessBacklog> getBacklog(final List<Map<String, String>> statuses,
                                    final String warehouseId,
                                    final ZonedDateTime dateFrom,
                                    final ZonedDateTime dateTo);

    List<Backlog> getSalesByCpt(final String warehouseId, final String dateCreatedFrom);

    ProcessBacklog getUnitBacklog(final UnitProcessBacklogInput input);

}
