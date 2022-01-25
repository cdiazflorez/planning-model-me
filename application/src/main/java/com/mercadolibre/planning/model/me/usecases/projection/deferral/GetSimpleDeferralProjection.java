package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class GetSimpleDeferralProjection implements
        UseCase<GetProjectionInput, GetSimpleDeferralProjectionOutput> {

    private static final List<ProcessName> PROCESS_NAMES = List.of(PICKING, PACKING, PACKING_WALL);

    private static final int DEFERRAL_DAYS_TO_PROJECT = 3;

    private final LogisticCenterGateway logisticCenterGateway;

    private final PlanningModelGateway planningModelGateway;

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

    private List<ProjectionResult> getSortedDeferralProjections(final GetProjectionInput input,
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
