package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.entities.workflows.Step.GROUPED;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.GROUPING;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.PENDING;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.PICKED;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.SORTED;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.TO_GROUP;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.TO_PACK;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.TO_PICK;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.TO_ROUTE;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.TO_SORT;
import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;

/** Configuration for SLA projections. */
@AllArgsConstructor
public enum ProjectionWorkflow {
  FBM_WMS_OUTBOUND(
      of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group", "grouping", "grouped", "to_pack"),
      of(PACKING, PACKING_WALL),
      Set.of(PENDING, TO_ROUTE, TO_PICK, PICKED, TO_SORT, SORTED, TO_GROUP, GROUPING, GROUPED, TO_PACK)),
  FBM_WMS_INBOUND(
      of("check_in", "put_away"),
      of(CHECK_IN, PUT_AWAY),
      Set.of(Step.CHECK_IN, Step.PUT_AWAY));

  private List<String> statuses;

  private List<ProcessName> processes;

  private Set<Step> steps;

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

  /**
   * Returns the steps with which the projection should be run.
   *
   * @param  workflow target workflow.
   * @return          the steps.
   */
  public static Set<Step> getSteps(final Workflow workflow) {
    return from(workflow).steps;
  }
}
