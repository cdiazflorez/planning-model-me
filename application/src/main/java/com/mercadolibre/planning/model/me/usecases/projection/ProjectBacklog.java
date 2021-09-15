package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest.fromInput;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;

@Named
@AllArgsConstructor
public class ProjectBacklog implements UseCase<BacklogProjectionInput, ProjectedBacklog> {

    private final PlanningModelGateway planningModel;

    private final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public ProjectedBacklog execute(final BacklogProjectionInput input) {
        final List<CurrentBacklog> backlogs = getBacklogList(input);
        return new ProjectedBacklog(
                planningModel.getBacklogProjection(fromInput(input, backlogs))
        );
    }

    private List<CurrentBacklog> getBacklogList(final BacklogProjectionInput input) {
        final ZonedDateTime dateFrom = input.getDateFrom();
        final List<Map<String, String>> statuses = List.of(
                Map.of("status", OUTBOUND_PLANNING.getStatus()),
                Map.of("status", ProcessInfo.PACKING.getStatus())
        );

        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()));

        List<ProcessBacklog> backlogs =
                backlogGateway.getBacklog(statuses,
                        input.getWarehouseId(),
                        dateFrom,
                        input.getDateTo(),
                        true
                );

        final ProcessBacklog pickingBacklog =
                backlogGateway.getUnitBacklog(
                        new UnitProcessBacklogInput(ProcessInfo.PICKING.getStatus(),
                                input.getWarehouseId(),
                                dateFrom,
                                input.getDateTo(),
                                null,
                                input.getGroupType(),
                                true)
                );

        return List.of(
                new CurrentBacklog(WAVING, backlogs.stream()
                        .filter(t -> t.getProcess().equals(OUTBOUND_PLANNING.getStatus()))
                        .findFirst()
                        .map(ProcessBacklog::getQuantity)
                        .orElse(0)
                ),
                new CurrentBacklog(PICKING, pickingBacklog.getQuantity()),
                new CurrentBacklog(PACKING, backlogs.stream()
                        .filter(t -> t.getProcess().equals(ProcessInfo.PACKING.getStatus()))
                        .findFirst()
                        .map(ProcessBacklog::getQuantity)
                        .orElse(0))
        );
    }
}

