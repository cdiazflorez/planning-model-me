package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import org.apache.commons.math3.util.Precision;


@Named
@AllArgsConstructor
public class GetBacklogScheduled {
  private static final List<BacklogWorkflow> INBOUND_WORKFLOWS = List.of(BacklogWorkflow.INBOUND, BacklogWorkflow.INBOUND_TRANSFER);
  private static final String WORKFLOW_KEY = "workflow";
  private static final String DATE_IN_KEY = "date_in";
  private static final int SCALE_DECIMAL = 2;

  private static final int AMOUNT_TO_ADD_MINUTES = 5;

  private static final int AMOUNT_TO_ADD_DAYS = 1;

  private final LogisticCenterGateway logisticCenterGateway;

  private final BacklogApiGateway backlogGateway;

  public Map<String, BacklogScheduled> execute(String logisticCenterId, Instant requestDate) {
    final Instant today = ZonedDateTime.ofInstant(
            requestDate,
            logisticCenterGateway.getConfiguration(logisticCenterId).getZoneId()
        )
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();
    final Map<BacklogWorkflow, Integer> receivedBacklog = getReceivedInboundBacklog(
        logisticCenterId,
        today,
        requestDate
    );

    final Map<BacklogWorkflow, Map<Instant, Integer>> firstBacklogPhotoTaken = getFirstBacklogPhotoTaken(
        logisticCenterId,
        today
    );

    final var backlogScheduledMap = INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
            BacklogWorkflow::getJsonProperty,
            workflow -> calculateBacklogScheduled(
                firstBacklogPhotoTaken.get(workflow),
                receivedBacklog.get(workflow),
                requestDate
            )
        ));
    backlogScheduledMap.put("total", totalizeBacklogScheduled(
       backlogScheduledMap.get(BacklogWorkflow.INBOUND.getJsonProperty()),
       backlogScheduledMap.get(BacklogWorkflow.INBOUND_TRANSFER.getJsonProperty())
    ));

    return backlogScheduledMap;
  }

  private BacklogScheduled calculateBacklogScheduled(
      Map<Instant, Integer> firstBacklogPhotoTaken,
      int receivedBacklog,
      Instant requestDate
  ) {
    int expectedBacklog = 0;
    int remainBacklog = 0;
    for (Map.Entry<Instant, Integer>
        entry : firstBacklogPhotoTaken.entrySet()) {
      if (entry.getKey().isBefore(requestDate)) {
        expectedBacklog += entry.getValue();
      } else {
        remainBacklog += entry.getValue();
      }
    }
    return createBacklogScheduledResponse(expectedBacklog, receivedBacklog, remainBacklog);
  }

  private BacklogScheduled totalizeBacklogScheduled(
      BacklogScheduled inboundBacklog,
      BacklogScheduled inboundTransferBacklog
  ) {
    return createBacklogScheduledResponse(
        inboundBacklog.getExpected().getUnits() + inboundTransferBacklog.getExpected().getUnits(),
        inboundBacklog.getReceived().getUnits() + inboundTransferBacklog.getReceived().getUnits(),
        inboundBacklog.getPending().getUnits() + inboundTransferBacklog.getPending().getUnits()
    );
  }

  private BacklogScheduled createBacklogScheduledResponse(int backlogExpected, int receivedBacklog, int remainBacklog) {
    final int deviatedBacklog = backlogExpected - receivedBacklog;

    return new BacklogScheduled(
        Indicator.builder().units(backlogExpected).build(),
        Indicator.builder().units(receivedBacklog).build(),
        Indicator.builder().units(remainBacklog).build(),
        Indicator.builder()
            .units(deviatedBacklog)
            .percentage(getDeviationPercentage(deviatedBacklog, backlogExpected))
            .build());
  }

  private double getDeviationPercentage(int deviation, int backlogExpected) {
    if (backlogExpected != 0) {
      double percentage = ((double) deviation / (double) backlogExpected) * (-1);
      return Precision.round(percentage, SCALE_DECIMAL);
    }
    return 0;
  }

  /**
   * Get the total number of units corresponding to shipments whose scheduled arrival date is between the specified dates, according to the last backlog photo
   */
  private Map<BacklogWorkflow, Integer> getReceivedInboundBacklog(String warehouse, Instant scheduledDateFrom, Instant scheduledDateTo) {
    final List<Consolidation> receivedInboundBacklog = backlogGateway.getCurrentBacklog(
        new BacklogCurrentRequest(warehouse)
            .withDateInRange(scheduledDateFrom, scheduledDateTo)
            .withWorkflows(INBOUND_WORKFLOWS.stream().map(BacklogWorkflow::getName).collect(Collectors.toList()))
            .withSteps(List.of("CHECK_IN", "PUT_AWAY", "FINISHED"))
            .withGroupingFields(List.of("process", WORKFLOW_KEY)));

    return INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
            workflow -> workflow,
            workflow -> receivedInboundBacklog.stream()
                .filter(c -> c.getKeys().get(WORKFLOW_KEY).equals(workflow.getName()))
                .mapToInt(Consolidation::getTotal).sum()
        ));
  }

  private Map<BacklogWorkflow, Map<Instant, Integer>> getFirstBacklogPhotoTaken(String warehouse, Instant since) {
    return this.getFirstPhotoOfDay(warehouse, since).entrySet().stream().collect(
        Collectors.toMap(
            Map.Entry::getKey,
            v -> v.getValue().stream().collect(
                Collectors.toMap(
                  consolidation -> Instant.parse(consolidation.getKeys().get(DATE_IN_KEY)),
                  Consolidation::getTotal,
                  (firstPhotoValue, secondPhotoValue) -> firstPhotoValue
                )
            ))
    );
  }

  /**
   * Get number of units grouped by arrival date that were scheduled to arrive during a day according to the first photo taken that day.
   */
  private Map<BacklogWorkflow, List<Consolidation>> getFirstPhotoOfDay(String warehouseId, Instant dayDate) {
    final Instant photoDateTo = dayDate.plus(AMOUNT_TO_ADD_MINUTES, ChronoUnit.MINUTES);
    List<Consolidation> photos = backlogGateway.getBacklog(
        new BacklogRequest(warehouseId, dayDate, photoDateTo)
            .withWorkflows(INBOUND_WORKFLOWS.stream().map(BacklogWorkflow::getName).collect(Collectors.toList()))
            .withSteps(List.of("SCHEDULED"))
            .withGroupingFields(List.of(DATE_IN_KEY, WORKFLOW_KEY))
            .withDateInRange(dayDate, dayDate.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS))
    );

    final var consolidationsByWorkflow = INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
            workflow -> workflow,
            workflow -> photos.stream()
                .filter(c -> c.getKeys().get(WORKFLOW_KEY).equals(workflow.getName()))
                .collect(
                    Collectors.groupingBy(
                        Consolidation::getDate,
                        TreeMap::new,
                        Collectors.toList()
                    )
                )
        ));

    return consolidationsByWorkflow.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            v -> v.getValue().isEmpty()
                ? List.of()
                : v.getValue().firstEntry().getValue()
        ));
  }
}
