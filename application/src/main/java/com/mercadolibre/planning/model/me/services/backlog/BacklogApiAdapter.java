package com.mercadolibre.planning.model.me.services.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

@Deprecated
@Slf4j
@Named
@AllArgsConstructor
public class BacklogApiAdapter {

    private final BacklogApiGateway backlogApiGateway;

    private final ProjectBacklog backlogProjection;

    private static final String OUTBOUND_ORDERS = "outbound-orders";
    private static final String INBOUND = "inbound";

    private static final Map<Workflow, String> WORKFLOW_BY_ALIAS_WORKFLOW = Map.of(
            FBM_WMS_OUTBOUND, OUTBOUND_ORDERS,
            FBM_WMS_INBOUND, INBOUND
    );

    private static final Map<Workflow, String> GROUP_TYPE_BY_WORKFLOW = Map.of(
            FBM_WMS_OUTBOUND, "order",
            FBM_WMS_INBOUND, ""
    );

    public List<Consolidation> getCurrentBacklog(final Instant requestDate,
                                                 final String warehouseId,
                                                 final List<Workflow> workflows,
                                                 final List<ProcessName> processes,
                                                 final List<BacklogGrouper> groupings,
                                                 final Instant dateFrom,
                                                 final Instant dateTo,
                                                 final Instant slaFrom,
                                                 final Instant slaTo) {

        final BacklogRequest adapterRequest = new BacklogRequest(warehouseId, dateFrom, dateTo)
            .withRequestDate(requestDate)
            .withWorkflows(getWorkflowAliasByWorkflow(workflows))
            .withProcesses(processes.stream()
                               .map(ProcessName::getName)
                               .collect(Collectors.toList())
            )
            .withSteps(Collections.emptyList())
            .withGroupingFields(groupings.stream().map(BacklogGrouper::getName).collect(Collectors.toList()))
            .withSlaRange(slaFrom, slaTo);

        return backlogApiGateway.getBacklog(adapterRequest);
    }

    @Deprecated
    public List<BacklogProjectionResponse> getProjectedBacklog(final String warehouseId,
                                                               final Workflow workflow,
                                                               final List<ProcessName> processes,
                                                               final ZonedDateTime dateFrom,
                                                               final ZonedDateTime dateTo,
                                                               final Long userId,
                                                               final List<Consolidation> currentBacklog) {

        return backlogProjection.execute(
                        BacklogProjectionInput.builder()
                                .workflow(workflow)
                                .warehouseId(warehouseId)
                                .processName(processes)
                                .dateFrom(dateFrom)
                                .dateTo(dateTo)
                                .groupType(GROUP_TYPE_BY_WORKFLOW.get(workflow))
                                .userId(userId)
                                .build())
                .getProjections();
    }

    private List<String> getWorkflowAliasByWorkflow(final List<Workflow> workflowsBase) {

        return workflowsBase.stream().map(WORKFLOW_BY_ALIAS_WORKFLOW::get)
                .collect(Collectors.toList());
    }
}
