package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType.CPT;

@Named
public class GetCptProjection extends GetProjection {

    public GetCptProjection(final PlanningModelGateway planningModelGateway,
                            final LogisticCenterGateway logisticCenterGateway,
                            final GetWaveSuggestion getWaveSuggestion,
                            final GetEntities getEntities,
                            final GetProjectionSummary getProjectionSummary,
                            final GetBacklog getBacklog) {

        super(planningModelGateway, logisticCenterGateway, getWaveSuggestion, getEntities,
                getProjectionSummary, getBacklog);
    }

    @Override
    protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                            final ZonedDateTime dateFrom,
                                            final ZonedDateTime dateTo,
                                            final List<Backlog> backlogs) {

        return planningModelGateway.runProjection(ProjectionRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(input.getWorkflow())
                        .processName(PROJECTION_PROCESS_NAMES)
                        .type(CPT)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .backlog(backlogs)
                        .userId(input.getUserId())
                        .applyDeviation(true)
                        .build());
    }


}
