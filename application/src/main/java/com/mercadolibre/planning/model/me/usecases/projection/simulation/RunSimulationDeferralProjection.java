package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.clock.RequestClockGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.usecases.projection.GeneralDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import javax.inject.Named;

@Named
public class RunSimulationDeferralProjection extends GeneralDeferralProjection {

    public RunSimulationDeferralProjection(PlanningModelGateway planningModelGateway,
                                           GetProjectionSummary getProjectionSummary,
                                           GetSimulationDeferralProjection getSimpleDeferralProjection,
                                           BacklogApiGateway backlogGateway,
                                           RequestClockGateway requestClockGateway) {
        super(planningModelGateway, getProjectionSummary, getSimpleDeferralProjection, backlogGateway, requestClockGateway);
    }
}
