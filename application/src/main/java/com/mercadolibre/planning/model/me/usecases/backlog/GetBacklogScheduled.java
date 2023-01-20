package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogScheduled;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import org.apache.commons.math3.util.Precision;


@Named
@AllArgsConstructor
public class GetBacklogScheduled {
  private static final Set<BacklogWorkflow> INBOUND_WORKFLOWS = Set.of(BacklogWorkflow.INBOUND, BacklogWorkflow.INBOUND_TRANSFER);
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
    final Photo photo = backlogGateway.getLastPhoto(
        new BacklogLastPhotoRequest(
            warehouse,
            INBOUND_WORKFLOWS,
            Set.of(Step.CHECK_IN, Step.PUT_AWAY, Step.FINISHED),
            scheduledDateFrom,
            scheduledDateTo,
            null,
            null,
            Set.of(BacklogGrouper.PROCESS, BacklogGrouper.WORKFLOW),
            Instant.now()
        ));

    final List<Photo.Group> receivedInboundBacklog = photo == null ? Collections.emptyList() : photo.getGroups();


    return INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
            workflow -> workflow,
            workflow -> receivedInboundBacklog.stream()
                .filter(c -> c.getKey().get(BacklogGrouper.WORKFLOW).equals(workflow.getName()))
                .mapToInt(Photo.Group::getTotal).sum()
        ));
  }

  private Map<BacklogWorkflow, Map<Instant, Integer>> getFirstBacklogPhotoTaken(String warehouse, Instant since) {
    return this.getFirstPhotoOfDay(warehouse, since)
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                v -> v.getValue().stream().collect(
                    Collectors.toMap(
                        photoGroup -> Instant.parse(photoGroup.getKey().get(BacklogGrouper.DATE_IN)),
                        Photo.Group::getTotal,
                        (firstPhotoValue, secondPhotoValue) -> firstPhotoValue
                    )
                ))
        );
  }

  /**
   * Get number of units grouped by arrival date that were scheduled to arrive during a day according to the first photo taken that day.
   */
  private Map<BacklogWorkflow, List<Photo.Group>> getFirstPhotoOfDay(String warehouseId, Instant dayDate) {
    final Instant photoDateTo = dayDate.plus(AMOUNT_TO_ADD_MINUTES, ChronoUnit.MINUTES);
    List<Photo> photos = backlogGateway.getPhotos(new BacklogPhotosRequest(
        warehouseId,
        INBOUND_WORKFLOWS,
        Set.of(Step.SCHEDULED),
        dayDate,
        dayDate.plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS),
        null,
        null,
        Set.of(BacklogGrouper.DATE_IN, BacklogGrouper.WORKFLOW),
        dayDate,
        photoDateTo
    ));

    final List<Photo.Group> groups = photos.stream().min(Comparator.comparing(Photo::getTakenOn)).map(Photo::getGroups).orElse(List.of());

    return INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
            workflow -> workflow,
            workflow -> groups.stream()
                .filter(group -> group.getKey().get(BacklogGrouper.WORKFLOW).equals(workflow.getName()))
                .collect(Collectors.toList())
        ));
  }
}
