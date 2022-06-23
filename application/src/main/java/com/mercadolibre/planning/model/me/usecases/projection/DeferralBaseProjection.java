package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class DeferralBaseProjection implements UseCase<GetProjectionInput, GetSimpleDeferralProjectionOutput> {

    protected static final List<ProcessName> PROCESS_NAMES = List.of(PICKING, PACKING, PACKING_WALL);

    private static final int DEFERRAL_DAYS_TO_PROJECT = 3;

    protected final PlanningModelGateway planningModelGateway;

    protected final ProjectionGateway projectionGateway;

    private final LogisticCenterGateway logisticCenterGateway;


    public GetSimpleDeferralProjectionOutput execute(final GetProjectionInput input) {

        final ZonedDateTime dateFromToProject = getCurrentUtcDate();
        final ZonedDateTime dateToToProject = dateFromToProject.plusDays(DEFERRAL_DAYS_TO_PROJECT);

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getLogisticCenterId());

        List<ProjectionResult> deferralProjections =
                getSortedDeferralProjections(input, dateFromToProject, dateToToProject,
                        input.getBacklogToProject(), config.getTimeZone().getID());

        return new GetSimpleDeferralProjectionOutput(deferralProjections, config);
    }

    public abstract List<ProjectionResult> getSortedDeferralProjections(GetProjectionInput input,
                                                                        ZonedDateTime dateFrom,
                                                                        ZonedDateTime dateTo,
                                                                        List<Backlog> backlogs,
                                                                        String timeZone);
}
