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
  private static final int SCALE_DECIMAL = 2;

  private static final int AMOUNT_TO_ADD_MINUTES = 5;

  private static final int AMOUNT_TO_ADD_DAYS = 1;

  private final LogisticCenterGateway logisticCenterGateway;

  private final BacklogApiGateway backlogGateway;

  public BacklogScheduled execute(String logisticCenterId, Instant requestDate) {
    final Instant today = ZonedDateTime.ofInstant(
            requestDate,
            logisticCenterGateway.getConfiguration(logisticCenterId).getZoneId()
        )
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();
    final int receivedBacklog = this.getReceivedInboundBacklog(logisticCenterId, today, requestDate);
    final Map<Instant, Integer> firstBacklogPhotoTaken = getFirstBacklogPhotoTaken(logisticCenterId, today);
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
  private int getReceivedInboundBacklog(String warehouse, Instant scheduledDateFrom, Instant scheduledDateTo) {
    final List<Consolidation> receivedInboundBacklog = backlogGateway.getCurrentBacklog(
        new BacklogCurrentRequest(warehouse)
            .withDateInRange(scheduledDateFrom, scheduledDateTo)
            .withWorkflows(List.of(BacklogWorkflow.INBOUND.getName(), BacklogWorkflow.INBOUND_TRANSFER.getName()))
            .withSteps(List.of("CHECK_IN", "PUT_AWAY", "FINISHED"))
            .withGroupingFields(List.of("process")));

    return receivedInboundBacklog.stream().mapToInt(Consolidation::getTotal).sum();
  }

  private Map<Instant, Integer> getFirstBacklogPhotoTaken(String warehouse, Instant since) {
    return this.getFirstPhotoOfDay(warehouse, since).stream().collect(
        Collectors.toMap(
            consolidation -> Instant.parse(consolidation.getKeys().get("date_in")),
            Consolidation::getTotal,
            (firstPhotoValue, secondPhotoValue) -> firstPhotoValue
        )
    );
  }

  /**
   * Get number of units grouped by arrival date that were scheduled to arrive during a day according to the first photo taken that day.
   */
  private List<Consolidation> getFirstPhotoOfDay(String warehouseId, Instant dayDate) {
    final Instant photoDateTo = dayDate.plus(AMOUNT_TO_ADD_MINUTES, ChronoUnit.MINUTES);
    List<Consolidation> photos = backlogGateway.getBacklog(
        new BacklogRequest(warehouseId, dayDate, photoDateTo)
            .withWorkflows(List.of(BacklogWorkflow.INBOUND.getName(), BacklogWorkflow.INBOUND_TRANSFER.getName()))
            .withSteps(List.of("SCHEDULED"))
            .withGroupingFields(List.of("date_in"))
            .withDateInRange(dayDate, dayDate.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS))
    );
    final TreeMap<Instant, List<Consolidation>> consolidations = photos.stream().collect(
        Collectors.groupingBy(
            Consolidation::getDate,
            TreeMap::new,
            Collectors.toList()
        ));
    return consolidations.isEmpty() ? List.of() : consolidations.firstEntry().getValue();
  }
}
