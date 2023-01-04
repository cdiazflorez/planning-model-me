package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.entities.workflows.Step.CHECK_IN;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.FINISHED;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.PUT_AWAY;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.SCHEDULED;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.BacklogScheduledMetrics;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.ProcessMetric;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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

  private static final int AMOUNT_TO_ADD_DAYS = 1;

  private static final int AMOUNT_TO_SUBTRACT_MINUTES = 1;

  private final LogisticCenterGateway logisticCenterGateway;

  private final BacklogApiGateway backlogGateway;

  public InboundBacklogMonitor execute(final String logisticCenterId, final Instant requestDate) {
    final Instant firstHourOfDayByLogisticCenter = ZonedDateTime.ofInstant(
            requestDate,
            logisticCenterGateway.getConfiguration(logisticCenterId).getZoneId()
        )
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();

    final List<Photo.Group> lastPhoto = getLastPhoto(logisticCenterId, requestDate);

    final List<InboundBacklogScheduled> scheduledBacklogs = getScheduledBacklog(
        lastPhoto,
        requestDate,
        firstHourOfDayByLogisticCenter
    );

    return new InboundBacklogMonitor(
        requestDate,
        scheduledBacklogs,
        getProcessMetric(filterByStep(lastPhoto, CHECK_IN), requestDate),
        getProcessMetric(filterByStep(lastPhoto, PUT_AWAY), requestDate)
    );
  }

  private List<InboundBacklogScheduled> getScheduledBacklog(
      final List<Photo.Group> inboundBacklog,
      final Instant requestDate,
      final Instant firstHourOfDayByWarehouse
  ) {
    final Instant lastHourOfDayByWarehouse = firstHourOfDayByWarehouse
        .plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS)
        .minus(AMOUNT_TO_SUBTRACT_MINUTES, ChronoUnit.MINUTES);

    final Instant tomorrow = firstHourOfDayByWarehouse
        .plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS);

    final Instant tomorrowLastHour = lastHourOfDayByWarehouse
        .plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS);

    final InboundBacklogScheduled scheduledBacklogToNow = calculateBacklogByDate(
        firstHourOfDayByWarehouse,
        requestDate,
        inboundBacklog,
        of(SCHEDULED, CHECK_IN, PUT_AWAY, FINISHED),
        true
    );

    final InboundBacklogScheduled scheduledBacklogToday = calculateBacklogByDate(
        firstHourOfDayByWarehouse,
        lastHourOfDayByWarehouse,
        inboundBacklog,
        of(SCHEDULED, CHECK_IN, PUT_AWAY, FINISHED),
        false
    );

    final InboundBacklogScheduled scheduledBacklogTomorrow = calculateBacklogByDate(
        tomorrow,
        tomorrowLastHour,
        inboundBacklog,
        of(SCHEDULED),
        false
    );

    return of(scheduledBacklogToNow, scheduledBacklogToday, scheduledBacklogTomorrow);
  }

  private InboundBacklogScheduled calculateBacklogByDate(
      final Instant dateFrom,
      final Instant dateTo,
      final List<Photo.Group> inboundBacklogGrouped,
      final List<Step> steps,
      final boolean isFirstCard
  ) {
    final List<String> stepsNames = steps.stream().map(Step::getName).collect(Collectors.toList());

    final Map<BacklogWorkflow, Integer> expectedBacklogByWorkflow = INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
                workflow -> workflow,
                workflow -> inboundBacklogGrouped.stream()
                    .filter(group -> group.getKey().get(BacklogGrouper.WORKFLOW).equals(workflow.getName())
                        && isDateBetween(dateFrom, dateTo, Instant.parse(group.getKey().get(BacklogGrouper.DATE_IN)))
                        && stepsNames.contains(group.getKey().get(BacklogGrouper.STEP)))
                    .mapToInt(Photo.Group::getTotal).sum()
            )
        );

    final Map<BacklogWorkflow, Integer> scheduledBacklogByWorkflow = INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
                workflow -> workflow,
                workflow -> inboundBacklogGrouped.stream()
                    .filter(group -> group.getKey().get(BacklogGrouper.WORKFLOW).equals(workflow.getName())
                        && isDateBetween(dateFrom, dateTo, Instant.parse(group.getKey().get(BacklogGrouper.DATE_IN)))
                        && SCHEDULED.getName().equals(group.getKey().get(BacklogGrouper.STEP)))
                    .mapToInt(Photo.Group::getTotal).sum()
            )
        );

    final BacklogScheduledMetrics inboundBacklog = createBacklogScheduledResponse(
        expectedBacklogByWorkflow.get(BacklogWorkflow.INBOUND),
        expectedBacklogByWorkflow.get(BacklogWorkflow.INBOUND) - scheduledBacklogByWorkflow.get(BacklogWorkflow.INBOUND),
        isFirstCard
    );

    final BacklogScheduledMetrics inboundTransferBacklog = createBacklogScheduledResponse(
        expectedBacklogByWorkflow.get(BacklogWorkflow.INBOUND_TRANSFER),
        expectedBacklogByWorkflow.get(BacklogWorkflow.INBOUND_TRANSFER) - scheduledBacklogByWorkflow.get(BacklogWorkflow.INBOUND_TRANSFER),
        isFirstCard
    );

    return new InboundBacklogScheduled(
        dateFrom.truncatedTo(ChronoUnit.MINUTES),
        dateTo.truncatedTo(ChronoUnit.MINUTES),
        inboundBacklog,
        inboundTransferBacklog,
        totalizeBacklogScheduled(inboundBacklog, inboundTransferBacklog, isFirstCard)
    );
  }

  private boolean isDateBetween(final Instant dateFrom, final Instant dateTo, final Instant date) {
    return !date.isBefore(dateFrom) && !date.isAfter(dateTo);
  }


  private BacklogScheduledMetrics totalizeBacklogScheduled(
      final BacklogScheduledMetrics inboundBacklog,
      final BacklogScheduledMetrics inboundTransferBacklog,
      final boolean isFirstCard
  ) {
    return createBacklogScheduledResponse(
        inboundBacklog.getExpected().getUnits() + inboundTransferBacklog.getExpected().getUnits(),
        isFirstCard ? inboundBacklog.getReceived().getUnits() + inboundTransferBacklog.getReceived().getUnits() : 0,
        isFirstCard
    );
  }

  private BacklogScheduledMetrics createBacklogScheduledResponse(final int backlogExpected,
                                                                 final int receivedBacklog,
                                                                 final boolean isFirstCard) {
    final int deviatedBacklog = backlogExpected - receivedBacklog;

    final Indicator receiving = isFirstCard ? Indicator.builder().units(receivedBacklog).build() : null;
    final Indicator deviation = isFirstCard
        ? Indicator.builder()
        .units(deviatedBacklog)
        .percentage(getDeviationPercentage(deviatedBacklog, backlogExpected))
        .build()
        : null;

    return new BacklogScheduledMetrics(
        Indicator.builder().units(backlogExpected).build(),
        receiving,
        deviation);
  }

  private double getDeviationPercentage(final int deviation, final int backlogExpected) {
    if (backlogExpected != 0) {
      double percentage = ((double) deviation / (double) backlogExpected) * (-1);
      return Precision.round(percentage, SCALE_DECIMAL);
    }
    return 0;
  }

  private List<Photo.Group> getLastPhoto(final String warehouse, final Instant today) {

    final Photo photo = backlogGateway.getLastPhoto(
        new BacklogLastPhotoRequest(
            warehouse,
            INBOUND_WORKFLOWS,
            Set.of(SCHEDULED, CHECK_IN, PUT_AWAY, FINISHED),
            null,
            null,
            null,
            null,
            Set.of(BacklogGrouper.STEP, BacklogGrouper.DATE_OUT, BacklogGrouper.WORKFLOW, BacklogGrouper.DATE_IN),
            today
        ));

    return photo == null ? Collections.emptyList() : photo.getGroups();
  }

  private List<Photo.Group> filterByStep(final List<Photo.Group> inboundBacklog, final Step step) {
    return inboundBacklog.stream()
        .filter(group -> step.getName().equalsIgnoreCase(group.getKey().get(BacklogGrouper.STEP)))
        .collect(Collectors.toList());
  }

  private ProcessMetric getProcessMetric(final List<Photo.Group> groups, final Instant today) {

    final Instant tomorrow = today.plus(1, ChronoUnit.DAYS);
    final int immediateBacklog = groups.stream()
        .filter(group -> Instant.parse(group.getKey().get(BacklogGrouper.DATE_OUT)).isBefore(tomorrow))
        .mapToInt(Photo.Group::getTotal).sum();

    final int totalBacklog = groups.stream().mapToInt(Photo.Group::getTotal).sum();

    return new ProcessMetric(Indicator.builder().units(totalBacklog).build(), Indicator.builder().units(immediateBacklog).build());
  }
}
