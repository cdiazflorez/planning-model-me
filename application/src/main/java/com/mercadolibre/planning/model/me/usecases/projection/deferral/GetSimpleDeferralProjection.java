package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.usecases.projection.DeferralBaseProjection;
import java.time.ZonedDateTime;
import java.util.List;
import javax.inject.Named;

@Named
public class GetSimpleDeferralProjection extends DeferralBaseProjection {


    public GetSimpleDeferralProjection(LogisticCenterGateway logisticCenterGateway,
                                       PlanningModelGateway planningModelGateway,
                                       ProjectionGateway projectionGateway) {
        super(logisticCenterGateway, planningModelGateway, projectionGateway);
    }

    public List<ProjectionResult> getSortedDeferralProjections(final GetProjectionInput input,
                                                               final ZonedDateTime dateFrom,
                                                               final ZonedDateTime dateTo,
                                                               final List<Backlog> backlogs,
                                                               final String timeZone) {

        final List<ProjectionResult> projection = planningModelGateway.runDeferralProjection(
                ProjectionRequest.builder()
                        .warehouseId(input.getLogisticCenterId())
                        .workflow(input.getWorkflow())
                        .processName(PROCESS_NAMES)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .backlog(backlogs)
                        .timeZone(timeZone)
                        .build());

        return projection.stream()
                .sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate()))
                .collect(toList());
    }
}
