package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.projection.DeferralBaseProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import java.time.ZonedDateTime;
import java.util.List;
import javax.inject.Named;

@Named
public class GetSimulationDeferralProjection extends DeferralBaseProjection {

    public GetSimulationDeferralProjection(
            LogisticCenterGateway logisticCenterGateway,
            PlanningModelGateway planningModelGateway) {
        super(logisticCenterGateway, planningModelGateway);
    }

    @Override
    public List<ProjectionResult> getSortedDeferralProjections(
            GetProjectionInput input, ZonedDateTime dateFrom,
            ZonedDateTime dateTo, List<Backlog> backlogs,
            String timeZone) {

        final List<ProjectionResult> projection = planningModelGateway.runSimulationDeferralProjection(
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
