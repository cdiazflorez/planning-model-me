package com.mercadolibre.planning.model.me.services.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

@Slf4j
@Named
@AllArgsConstructor
public class BacklogApiAdapter {

    private final BacklogApiGateway backlogApiGateway;

    private static final String OUTBOUND_ORDERS = "outbound-orders";
    private static final String INBOUND = "inbound";

    private static final Map<Workflow, String> WORKFLOW_BY_ALIAS_WORKFLOW = Map.of(
            FBM_WMS_OUTBOUND, OUTBOUND_ORDERS,
            FBM_WMS_INBOUND, INBOUND
    );

    public List<Consolidation> execute(final Instant requestDate,
                                       final String warehouseId,
                                       final List<Workflow> workflows,
                                       final List<ProcessName> processes,
                                       final List<String> groupingFields,
                                       final Instant dateFrom,
                                       final Instant dateTo) {

        final BacklogRequest adapterRequest = new BacklogRequest(
                requestDate,
                warehouseId,
                getWorkflowByWorkflow(workflows),
                processes.stream()
                        .map(ProcessName::getName)
                        .collect(Collectors.toList()),
                groupingFields,
                dateFrom,
                dateTo);

        return backlogApiGateway.getBacklog(adapterRequest);
    }

    private List<String> getWorkflowByWorkflow(final List<Workflow> workflowsBase) {

        return workflowsBase.stream().map(WORKFLOW_BY_ALIAS_WORKFLOW::get)
                .collect(Collectors.toList());
    }
}
