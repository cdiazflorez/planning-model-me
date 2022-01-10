package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest.fromInput;

@Named
@AllArgsConstructor
public class ProjectBacklog implements UseCase<BacklogProjectionInput, ProjectedBacklog> {

    private final Map<Workflow, List<Map<String, String>>> statusByWorkflow = Map.of(
            FBM_WMS_OUTBOUND, List.of(
                    Map.of("status", ProcessOutbound.OUTBOUND_PLANNING.getStatus()),
                    Map.of("status", ProcessOutbound.PICKING.getStatus()),
                    Map.of("status", ProcessOutbound.PACKING.getStatus())));

    private final PlanningModelGateway planningModel;

    private final BacklogGatewayProvider backlogGatewayProvider;

    @Override
    public ProjectedBacklog execute(final BacklogProjectionInput input) {

        final List<CurrentBacklog> backlogs = input.getWorkflow() == FBM_WMS_OUTBOUND
                ? getOutBoundProjectedBacklog(
                        statusByWorkflow.get(input.getWorkflow()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        input.getWarehouseId(),
                        input.getGroupType())
                : getInBoundProjectedBacklog(input.getCurrentBacklog());

        return new ProjectedBacklog(
                planningModel.getBacklogProjection(fromInput(input, backlogs))
        );
    }

    private List<CurrentBacklog> getOutBoundProjectedBacklog(final List<Map<String, String>> status,
                                                             final ZonedDateTime dateFrom,
                                                             final ZonedDateTime dateTo,
                                                             final String warehouse,
                                                             final String groupType) {

        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(FBM_WMS_OUTBOUND)
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(FBM_WMS_OUTBOUND));

        final List<ProcessBacklog> backlogs =
                backlogGateway.getBacklog(status, warehouse, dateFrom, dateTo, true);

        final ProcessBacklog pickingBacklog =
                backlogGateway.getUnitBacklog(
                        new UnitProcessBacklogInput(ProcessOutbound.PICKING.getStatus(),
                                warehouse,
                                dateFrom,
                                dateTo,
                                null,
                                groupType,
                                false));

        return List.of(
                new CurrentBacklog(WAVING, backlogs.stream()
                        .filter(t -> t.getProcess().equals(
                                ProcessOutbound.OUTBOUND_PLANNING.getStatus()))
                        .findFirst()
                        .map(ProcessBacklog::getQuantity)
                        .orElse(0)
                ),
                new CurrentBacklog(PICKING, pickingBacklog.getQuantity()),
                new CurrentBacklog(PACKING, backlogs.stream()
                        .filter(t -> t.getProcess().equals(ProcessOutbound.PACKING.getStatus()))
                        .findFirst()
                        .map(ProcessBacklog::getQuantity)
                        .orElse(0))
        );
    }

    private List<CurrentBacklog> getInBoundProjectedBacklog(final List<Consolidation> currentBacklog) {

        return List.of(
                new CurrentBacklog(CHECK_IN, currentBacklog.stream()
                        .filter(item -> CHECK_IN.getName().equals(item.getKeys().get("process")))
                        .findFirst()
                        .map(Consolidation::getTotal)
                        .orElse(0)
                ),
                new CurrentBacklog(PUT_AWAY, currentBacklog.stream()
                        .filter(item -> PUT_AWAY.getName().equals(item.getKeys().get("process")))
                        .findFirst()
                        .map(Consolidation::getTotal)
                        .orElse(0)
                )
        );
    }
}

