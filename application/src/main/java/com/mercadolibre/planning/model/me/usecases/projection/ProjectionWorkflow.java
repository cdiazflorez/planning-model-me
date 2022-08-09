package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.util.List;
import lombok.AllArgsConstructor;

/** Configuration for SLA projections. */
@AllArgsConstructor
public enum ProjectionWorkflow {
  FBM_WMS_OUTBOUND(of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group", "grouping",
      "grouped", "to_pack"),
      of(PACKING, PACKING_WALL)),
  FBM_WMS_INBOUND(of("check_in", "put_away"), of(CHECK_IN, PUT_AWAY));

  private List<String> statuses;

  private List<ProcessName> processes;

  private static ProjectionWorkflow from(final Workflow globalWorkflow) {
    return ProjectionWorkflow.valueOf(globalWorkflow.name());
  }

  /**
   * Returns the statuses that should be used to query for current backlog.
   *
   * @param  workflow target workflow.
   * @return          the backlog related statuses.
   */
  public static List<String> getStatuses(final Workflow workflow) {
    return from(workflow).statuses;
  }

  /**
   * Returns the processes with which the projection should be run.
   *
   * @param  workflow target workflow.
   * @return          the processes.
   */
  public static List<ProcessName> getProcesses(final Workflow workflow) {
    return from(workflow).processes;
  }
}
