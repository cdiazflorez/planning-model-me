package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType.CPT;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import java.time.ZonedDateTime;
import java.util.List;
import javax.inject.Named;

@Named
public class GetSlaProjectionInbound extends GetProjectionInbound {
  protected GetSlaProjectionInbound(final PlanningModelGateway planningModelGateway,
                                    final LogisticCenterGateway logisticCenterGateway,
                                    final GetEntities getEntities,
                                    final GetBacklogByDateInbound getBacklogByDateInbound,
                                    final GetSales getSales) {

    super(planningModelGateway, logisticCenterGateway, getEntities, getBacklogByDateInbound, getSales);
  }

  @Override
  protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs,
                                                 final LogisticCenterConfiguration config) {

    List<ProjectionResult> projectionResults = planningModelGateway.runProjection(ProjectionRequest.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processName(PROCESS_NAMES_INBOUND)
        .type(CPT)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .backlog(backlogs)
        .userId(input.getUserId())
        .applyDeviation(true)
        .timeZone(config.getTimeZone().getID())
        .build());

    projectionResults.forEach(projectionResult ->
        projectionResult.setExpired(
            projectionResult.getDate()
                .isBefore(ZonedDateTime.now()
                    .withZoneSameInstant(projectionResult.getDate().getZone()))));

    return projectionResults;
  }
}