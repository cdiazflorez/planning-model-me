package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo.Group;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.ScheduleAdjustment;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetActiveDeviations {

  private static final Set<Workflow> INBOUND_WORKFLOWS = Set.of(Workflow.INBOUND, Workflow.INBOUND_TRANSFER);

  private final BacklogApiGateway backlogApiGateway;

  private final DeviationGateway deviationGateway;

  /**
   * Runs over unitsByDateIn map and add all those units that are within range.
   *
   * @param unitsByDateIn  A map of units grouped by date_in
   * @param lowerDateRange lower limit of date range
   * @param upperDateRange upper limit of date range
   * @return sums of units that are within range.
   */
  private static Integer sumOfUnitsInRange(
      final Map<Instant, Integer> unitsByDateIn,
      final Instant lowerDateRange,
      final Instant upperDateRange
  ) {
    if (unitsByDateIn == null) {
      return 0;
    }

    return unitsByDateIn.entrySet().stream()
        .filter(entry -> DateUtils.isBetweenInclusive(entry.getKey(), lowerDateRange, upperDateRange))
        .map(Map.Entry::getValue)
        .reduce(0, Integer::sum);
  }

  private static int getDeviatedUnits(
      final Map<Workflow, Map<Instant, Integer>> unitsByWorkflowAndDateIn,
      final Deviation deviation
  ) {
    final int totalUnits = sumOfUnitsInRange(
        unitsByWorkflowAndDateIn.get(deviation.getWorkflow()),
        deviation.getDateFrom(),
        deviation.getDateTo()
    );

    return (int) (totalUnits * deviation.getValue());
  }

  private static Set<BacklogWorkflow> toBacklogWorkflow(Set<Workflow> workflows) {
    return workflows.stream()
        .map(Workflow::getBacklogWorkflow)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableSet());
  }

  public List<ScheduleAdjustment> execute(final String logisticCenterId, final Instant viewDate) {
    final List<Deviation> activeDeviations = deviationGateway.getActiveDeviations(INBOUND_WORKFLOWS, logisticCenterId, viewDate);

    if (activeDeviations == null || activeDeviations.isEmpty()) {
      return Collections.emptyList();
    }

    final Instant lowerRangeDate = activeDeviations.stream()
        .map(Deviation::getDateFrom)
        .min(Instant::compareTo)
        .orElseThrow();

    final Instant upperRangeDate = activeDeviations.stream()
        .map(Deviation::getDateTo)
        .max(Instant::compareTo)
        .orElseThrow();

    final Set<Workflow> workflowsWithActiveDeviations = activeDeviations.stream()
        .map(Deviation::getWorkflow)
        .collect(Collectors.toUnmodifiableSet());

    final var unitsByWorkflowAndDateIn = getUnitsByWorkflowAndDateIn(
        logisticCenterId,
        workflowsWithActiveDeviations,
        lowerRangeDate,
        upperRangeDate,
        viewDate
    );

    return activeDeviations.stream()
        .map(deviation -> new ScheduleAdjustment(
            List.of(deviation.getWorkflow()),
            deviation.getType(),
            Collections.emptyList(),
            deviation.getValue(),
            getDeviatedUnits(unitsByWorkflowAndDateIn, deviation),
            deviation.getDateFrom(),
            deviation.getDateTo()
        ))
        .collect(Collectors.toUnmodifiableList());
  }

  private Map<Workflow, Map<Instant, Integer>> getUnitsByWorkflowAndDateIn(
      final String logisticCenterId,
      final Set<Workflow> workflows,
      final Instant lowerRangeDate,
      final Instant upperRangeDate,
      final Instant viewDate
  ) {
    final Photo scheduledBacklogByDateIn = backlogApiGateway.getLastPhoto(
        new BacklogLastPhotoRequest(
            logisticCenterId,
            toBacklogWorkflow(workflows),
            Set.of(Step.SCHEDULED),
            lowerRangeDate,
            upperRangeDate,
            null,
            null,
            Set.of(BacklogGrouper.DATE_IN, BacklogGrouper.WORKFLOW),
            viewDate
        )
    );

    if (scheduledBacklogByDateIn == null || scheduledBacklogByDateIn.getGroups() == null) {
      return Collections.emptyMap();
    }

    return scheduledBacklogByDateIn.getGroups().stream()
        .collect(Collectors.groupingBy(
                group -> group.getWorkflow(BacklogGrouper.WORKFLOW).orElseThrow(),
                Collectors.toMap(
                    group -> group.getDateIn(BacklogGrouper.DATE_IN).orElseThrow(),
                    Group::getTotal
                )
            )
        );
  }
}
