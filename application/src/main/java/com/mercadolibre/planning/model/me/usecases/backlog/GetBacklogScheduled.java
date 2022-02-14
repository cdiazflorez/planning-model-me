package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;


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
    final int lateBacklog = this.getNumberOfUnitsThatAreScheduledToArriveBetween(logisticCenterId, today, requestDate);
    final Map<Instant, Integer> firstBacklogPhotoTaken = getFirstBacklogPhotoTaken(logisticCenterId, today);
    int backlogExpected = 0;
    int remainBacklog = 0;
    for (Map.Entry<Instant, Integer>
        entry : firstBacklogPhotoTaken.entrySet()) {
      if (entry.getKey().isBefore(requestDate)) {
        backlogExpected = backlogExpected + entry.getValue();
      } else {
        remainBacklog = remainBacklog + entry.getValue();
      }
    }
    return createBacklogScheduledResponse(backlogExpected, lateBacklog, remainBacklog);
  }

  private BacklogScheduled createBacklogScheduledResponse(int backlogExpected, int lateBacklog, int remainBacklog) {
    return new BacklogScheduled(
        Indicator.builder().units(backlogExpected)
            .build(),
        getReceivedBacklog(backlogExpected, lateBacklog),
        Indicator.builder().units(remainBacklog).build(),
        Indicator.builder().units(lateBacklog).percentage(getDeviationPercentage(lateBacklog, backlogExpected))
            .build());
  }

  private double getDeviationPercentage(int desvio, int backlogExpected) {
    if (backlogExpected != 0) {
      double percentage = ((double) desvio / (double) backlogExpected) * (-1);
      return new BigDecimal(String.valueOf(percentage))
          .setScale(SCALE_DECIMAL, RoundingMode.FLOOR)
          .doubleValue();
    }
    return 0;
  }

  /**
   * Get the total number of units corresponding to shipments whose scheduled arrival date is between the specified dates, according to the last backlog photo
   */
  private int getNumberOfUnitsThatAreScheduledToArriveBetween(String warehouse, Instant scheduledDateFrom, Instant scheduledDateTo) {
    final List<Consolidation> getBacklogScheduledBetween = backlogGateway.getCurrentBacklog(
        new BacklogCurrentRequest(warehouse)
            .withDateInRange(scheduledDateFrom, scheduledDateTo)
            .withWorkflows(List.of("inbound"))
            .withSteps(List.of("SCHEDULED"))
            .withGroupingFields(List.of("process")));
    return getBacklogScheduledBetween.stream()
        .mapToInt(Consolidation::getTotal)
        .sum();
  }

  private Indicator getReceivedBacklog(int expectedBacklog, int lateBacklog) {
    return Indicator
        .builder()
        .units(expectedBacklog - lateBacklog)
        .build();
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
            .withWorkflows(List.of("inbound"))
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
