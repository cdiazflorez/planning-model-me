package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Value;

/**
 * Class PhotoBacklog is used as currentPhotoBacklog and Photo in backlogHistorical.
 */
@Value
public class Photo {
  Instant takenOn;

  List<Group> groups;

  /**
   * Values per take_out.
   */
  @Value
  public static class Group {

    static Map<BacklogWorkflow, Workflow> toWorkflow = Map.of(
        BacklogWorkflow.INBOUND, Workflow.INBOUND,
        BacklogWorkflow.INBOUND_TRANSFER, Workflow.INBOUND_TRANSFER,
        BacklogWorkflow.OUTBOUND_ORDERS, Workflow.FBM_WMS_OUTBOUND
    );
    Map<BacklogGrouper, String> key;

    int total;

    // Also Known As accumulated immigration
    int accumulatedTotal;

    public Optional<String> getGroupValue(final BacklogGrouper grouper) {
      return Optional.ofNullable(key.get(grouper));
    }

    public Optional<Workflow> getWorkflow(final BacklogGrouper grouper) {
      return getGroupValue(grouper).flatMap(BacklogWorkflow::from).map(toWorkflow::get);
    }

    public Optional<BacklogWorkflow> getBacklogWorkflow(final BacklogGrouper grouper) {
      return getGroupValue(grouper).flatMap(BacklogWorkflow::from);
    }

    public Optional<Instant> getDateIn(final BacklogGrouper grouper) {
      return getGroupValue(grouper).map(Instant::parse);
    }

    public Optional<Step> getStep() {
      return Step.from(getKey().get(BacklogGrouper.STEP));
    }
  }
}

