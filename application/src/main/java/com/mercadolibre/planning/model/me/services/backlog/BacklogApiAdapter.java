package com.mercadolibre.planning.model.me.services.backlog;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Deprecated
@Slf4j
@Named
@AllArgsConstructor
public class BacklogApiAdapter {

  private static final Map<Workflow, String> GROUP_TYPE_BY_WORKFLOW = Map.of(
      FBM_WMS_OUTBOUND, "order",
      FBM_WMS_INBOUND, ""
  );

  private static final String INBOUND = "inbound";
  private static final String INBOUND_TRANSFER = "inbound-transfer";

  private static final String OUTBOUND_ORDERS = "outbound-orders";

  private static final Map<Workflow, List<String>> WORKFLOW_BY_ALIAS_WORKFLOW = Map.of(
      FBM_WMS_OUTBOUND, List.of(OUTBOUND_ORDERS),
      FBM_WMS_INBOUND, List.of(INBOUND, INBOUND_TRANSFER)
  );

  private final BacklogApiGateway backlogApiGateway;

  private final ProjectBacklog backlogProjection;

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
        .withProcesses(processes.stream().map(ProcessName::getName).collect(Collectors.toList()))
        .withSteps(Collections.emptyList())
        .withGroupingFields(groupings.stream().map(BacklogGrouper::getName).collect(Collectors.toList()))
        .withSlaRange(slaFrom, slaTo);

    return backlogApiGateway.getBacklog(adapterRequest);
  }

  public List<BacklogProjectionResponse> getProjectedBacklog(final String warehouseId,
                                                             final Workflow workflow,
                                                             final List<ProcessName> processesName,
                                                             final ZonedDateTime dateFrom,
                                                             final ZonedDateTime dateTo,
                                                             final Long userId) {

    return backlogProjection.execute(
        BacklogProjectionInput.builder()
            .workflow(workflow)
            .warehouseId(warehouseId)
            .processName(processesName)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .groupType(GROUP_TYPE_BY_WORKFLOW.get(workflow))
            .userId(userId)
            .build());
  }

  private List<String> getWorkflowAliasByWorkflow(final List<Workflow> workflowsBase) {
    return workflowsBase.stream()
        .map(WORKFLOW_BY_ALIAS_WORKFLOW::get)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
