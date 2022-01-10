package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType.CPT;

@Named
public class GetSlaProjectionInbound extends GetProjectionInbound {


    protected GetSlaProjectionInbound(final PlanningModelGateway planningModelGateway,
                                      final LogisticCenterGateway logisticCenterGateway,
                                      final GetWaveSuggestion getWaveSuggestion,
                                      final GetEntities getEntities,
                                      final GetProjectionSummary getProjectionSummary,
                                      final GetBacklogByDateInbound getBacklogByDateInbound) {

        super(planningModelGateway, logisticCenterGateway, getWaveSuggestion,
                getEntities, getProjectionSummary, getBacklogByDateInbound);
    }

    @Override
    protected List<ProjectionResult> getProjection(GetProjectionInputDto input, ZonedDateTime dateFrom,
                                                   ZonedDateTime dateTo, List<Backlog> backlogs, String timeZone) {
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
                .timeZone(timeZone)
                .build());

        projectionResults.forEach(projectionResult -> {
            ZonedDateTime now = ZonedDateTime.now()
                    .withZoneSameInstant(projectionResult.getDate().getZone());

            if (projectionResult.getDate()
                    .isBefore(now)) {
                projectionResult.setExpired(true);
                projectionResult.setDate(now.truncatedTo(ChronoUnit.HOURS));
            }
        });

        return projectionResults;

    }
}
