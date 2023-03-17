package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.entities.workflows.Step.CHECK_IN;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.PUT_AWAY;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.SCHEDULED;
import static com.mercadolibre.planning.model.me.entities.workflows.Step.getInboundSteps;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.inboundreports.InboundReportsApiGateway;
import com.mercadolibre.planning.model.me.gateways.inboundreports.dto.InboundResponse;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.BacklogScheduledMetrics;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogMonitor;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.InboundBacklogScheduled;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.ProcessMetric;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.Instant;
import java.time.ZoneId;
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

  private static final Set<Step> IB_STEPS_WITHOUT_SCHEDULED = getInboundSteps().stream().filter(step -> !step.equals(SCHEDULED))
      .collect(Collectors.toSet());

  private static final int SCALE_DECIMAL = 2;

  private static final int AMOUNT_TO_ADD_DAYS = 1;

  private static final int AMOUNT_TO_SUBTRACT_MINUTES = 1;

  private static final String UTC = "UTC";

  private static final long NOT_DEVIATION_APPLIED = 0;

  private final LogisticCenterGateway logisticCenterGateway;

  private final BacklogApiGateway backlogGateway;

  private final InboundReportsApiGateway inboundReportsApiGateway;

  private final PlanningModelGateway planningModelGateway;

  public InboundBacklogMonitor execute(final String logisticCenterId, final Instant requestDate) {
    final Instant firstHourOfDayByLogisticCenter = ZonedDateTime.ofInstant(
            requestDate,
           logisticCenterGateway.getConfiguration(logisticCenterId).getZoneId()
        )
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();

    final List<Photo.Group> lastPhoto = getLastPhoto(logisticCenterId, requestDate);

    final List<PlanningDistributionResponse> scheduledBacklogDeviatedSeller = planningModelGateway.getPlanningDistribution(
        generatePlanningDistributionRequest(logisticCenterId, INBOUND, firstHourOfDayByLogisticCenter)
    );

    final List<PlanningDistributionResponse> scheduledBacklogDeviatedTransfer = planningModelGateway.getPlanningDistribution(
        generatePlanningDistributionRequest(logisticCenterId, INBOUND_TRANSFER, firstHourOfDayByLogisticCenter));

    final List<InboundBacklogScheduled> scheduledBacklogs = getScheduledBacklog(
        lastPhoto,
        requestDate,
        firstHourOfDayByLogisticCenter,
        logisticCenterId,
        scheduledBacklogDeviatedSeller,
        scheduledBacklogDeviatedTransfer
    );

    return new InboundBacklogMonitor(
        requestDate,
        scheduledBacklogs.stream().sorted(Comparator.comparing(InboundBacklogScheduled::getDateTo)).collect(Collectors.toList()),
        getProcessMetric(filterByStep(lastPhoto, CHECK_IN), requestDate),
        getProcessMetric(filterByStep(lastPhoto, PUT_AWAY), requestDate)
    );
  }

  private PlanningDistributionRequest generatePlanningDistributionRequest(
      final String logisticCenterId, final Workflow workflow, final Instant firstHourOfDayByLogisticCenter) {
    final var dateInFrom = ZonedDateTime.ofInstant(firstHourOfDayByLogisticCenter, ZoneId.of(UTC));
    final var dateInTo = dateInFrom.plus(2, ChronoUnit.DAYS);
    final var dateOutFrom = dateInFrom.plus(1, ChronoUnit.DAYS);
    final var dateOutTo = dateInTo.plus(3, ChronoUnit.DAYS);

    return new PlanningDistributionRequest(
        logisticCenterId,
        workflow,
        dateInFrom,
        dateInTo,
        dateOutFrom,
        dateOutTo,
        true
    );
  }

  private List<InboundBacklogScheduled> getScheduledBacklog(
      final List<Photo.Group> inboundBacklog,
      final Instant requestDate,
      final Instant firstHourOfDayByWarehouse,
      final String logisticCenterId,
      final List<PlanningDistributionResponse> scheduledBacklogDeviatedSeller,
      final List<PlanningDistributionResponse> scheduledBacklogDeviatedTransfer
  ) {
    final Instant lastHourOfDayByWarehouse = firstHourOfDayByWarehouse
        .plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS)
        .minus(AMOUNT_TO_SUBTRACT_MINUTES, ChronoUnit.MINUTES);

    final Instant tomorrow = firstHourOfDayByWarehouse
        .plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS);

    final Instant tomorrowLastHour = lastHourOfDayByWarehouse
        .plus(AMOUNT_TO_ADD_DAYS, ChronoUnit.DAYS);

    final InboundBacklogScheduled scheduledBacklogToNow = calculateCurrentBacklog(
        firstHourOfDayByWarehouse,
        requestDate,
        inboundBacklog,
        logisticCenterId
    );

    final InboundBacklogScheduled scheduledBacklogToday = calculateNextBacklog(
        firstHourOfDayByWarehouse,
        lastHourOfDayByWarehouse,
        inboundBacklog,
        scheduledBacklogDeviatedSeller,
        scheduledBacklogDeviatedTransfer
    );

    final InboundBacklogScheduled scheduledBacklogTomorrow = calculateNextBacklog(
        tomorrow,
        tomorrowLastHour,
        inboundBacklog,
        scheduledBacklogDeviatedSeller,
        scheduledBacklogDeviatedTransfer
    );

    return of(scheduledBacklogToNow, scheduledBacklogToday, scheduledBacklogTomorrow);
  }

  private InboundBacklogScheduled calculateCurrentBacklog(
      final Instant dateFrom,
      final Instant dateTo,
      final List<Photo.Group> inboundBacklogGrouped,
      final String logisticCenterId
  ) {

    final var expectedBacklogByWorkflow = getBacklogByWorkflow(dateFrom, dateTo, inboundBacklogGrouped, getInboundSteps());
    final var receivingBacklogByWorkflow = getReceivedBacklogByWorkflow(dateFrom, dateTo, logisticCenterId);

    final BacklogScheduledMetrics inboundBacklog = createBacklogScheduledResponse(
        expectedBacklogByWorkflow.get(BacklogWorkflow.INBOUND),
        receivingBacklogByWorkflow.get(BacklogWorkflow.INBOUND)
    );

    final BacklogScheduledMetrics inboundTransferBacklog = createBacklogScheduledResponse(
        expectedBacklogByWorkflow.get(BacklogWorkflow.INBOUND_TRANSFER),
        receivingBacklogByWorkflow.get(BacklogWorkflow.INBOUND_TRANSFER)
    );

    return new InboundBacklogScheduled(
        dateFrom.truncatedTo(ChronoUnit.MINUTES),
        dateTo.truncatedTo(ChronoUnit.MINUTES),
        inboundBacklog,
        inboundTransferBacklog,
        totalizeBacklogScheduled(inboundBacklog, inboundTransferBacklog),
        NOT_DEVIATION_APPLIED
    );
  }

  private InboundBacklogScheduled calculateNextBacklog(
      final Instant dateFrom,
      final Instant dateTo,
      final List<Photo.Group> inboundBacklogGrouped,
      final List<PlanningDistributionResponse> scheduledBacklogSeller,
      final List<PlanningDistributionResponse> scheduledBacklogTransfer) {

    final var totalScheduledBacklogSeller = totalScheduledBacklogBetweenDate(scheduledBacklogSeller, dateFrom, dateTo);
    final var totalScheduledBacklogTransfer = totalScheduledBacklogBetweenDate(scheduledBacklogTransfer, dateFrom, dateTo);

    final var backlogReceivedByWorkflow = getBacklogByWorkflow(dateFrom, dateTo, inboundBacklogGrouped, IB_STEPS_WITHOUT_SCHEDULED);

    final Integer totalBacklogInbound = backlogReceivedByWorkflow.get(BacklogWorkflow.INBOUND) + totalScheduledBacklogSeller;
    final Integer totalBacklogInboundTransfer = backlogReceivedByWorkflow.get(BacklogWorkflow.INBOUND_TRANSFER)
        + totalScheduledBacklogTransfer;

    final BacklogScheduledMetrics inboundBacklog = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(totalBacklogInbound).build())
        .build();

    final BacklogScheduledMetrics inboundTransferBacklog = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(totalBacklogInboundTransfer).build())
        .build();

    final BacklogScheduledMetrics totalBacklog = BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(totalBacklogInbound + totalBacklogInboundTransfer).build())
        .build();

    final int totalBacklogScheduled = getBacklogByWorkflow(dateFrom, dateTo, inboundBacklogGrouped, Set.of(SCHEDULED))
        .values().stream().mapToInt(i -> i).sum();

    return new InboundBacklogScheduled(
        dateFrom.truncatedTo(ChronoUnit.MINUTES),
        dateTo.truncatedTo(ChronoUnit.MINUTES),
        inboundBacklog,
        inboundTransferBacklog,
        totalBacklog,
        Math.abs(totalScheduledBacklogSeller + totalScheduledBacklogTransfer - totalBacklogScheduled)
    );
  }

  private Map<BacklogWorkflow, Integer> getReceivedBacklogByWorkflow(final Instant dateFrom,
                                                                     final Instant dateTo,
                                                                     final String logisticCenterId) {

    final InboundResponse inboundResponseTotal = inboundReportsApiGateway.getUnitsReceived(logisticCenterId, dateFrom, dateTo, null);
    final InboundResponse inboundResponseTransfer = inboundReportsApiGateway.getUnitsReceived(
        logisticCenterId,
        dateFrom,
        dateTo,
        "transfer");

    final int total = getShippedQuantity(inboundResponseTotal);
    final int transfer = getShippedQuantity(inboundResponseTransfer);

    return Map.of(
        BacklogWorkflow.INBOUND, total - transfer,
        BacklogWorkflow.INBOUND_TRANSFER, transfer);
  }

  private int getShippedQuantity(final InboundResponse inboundResponse) {
    return inboundResponse.getAggregations().stream()
        .filter(aggregation -> "total_shipped_quantity".equalsIgnoreCase(aggregation.getName()))
        .mapToInt(InboundResponse.Aggregation::getValue)
        .findFirst()
        .orElse(0);
  }

  private Map<BacklogWorkflow, Integer> getBacklogByWorkflow(
      final Instant dateFrom,
      final Instant dateTo,
      final List<Photo.Group> inboundBacklogGrouped,
      final Set<Step> steps) {
    return INBOUND_WORKFLOWS.stream()
        .collect(Collectors.toMap(
                workflow -> workflow,
                workflow -> inboundBacklogGrouped.stream()
                    .filter(group -> workflow.equals(group.getBacklogWorkflow(BacklogGrouper.WORKFLOW).orElse(null))
                        && isDateBetween(dateFrom, dateTo, Instant.parse(group.getKey().get(BacklogGrouper.DATE_IN)))
                        && steps.contains(group.getStep().orElse(null)))
                    .mapToInt(Photo.Group::getTotal).sum()
            )
        );
  }

  private boolean isDateBetween(final Instant dateFrom, final Instant dateTo, final Instant date) {
    return !date.isBefore(dateFrom) && !date.isAfter(dateTo);
  }


  private BacklogScheduledMetrics totalizeBacklogScheduled(
      final BacklogScheduledMetrics inboundBacklog,
      final BacklogScheduledMetrics inboundTransferBacklog
  ) {
    return createBacklogScheduledResponse(
        inboundBacklog.getExpected().getUnits() + inboundTransferBacklog.getExpected().getUnits(),
        inboundBacklog.getReceived().getUnits() + inboundTransferBacklog.getReceived().getUnits()
    );
  }

  private BacklogScheduledMetrics createBacklogScheduledResponse(final int backlogExpected,
                                                                 final int receivedBacklog) {
    final int deviatedBacklog = backlogExpected - receivedBacklog;
    final Indicator deviation = Indicator.builder()
        .units(deviatedBacklog)
        .percentage(getDeviationPercentage(deviatedBacklog, backlogExpected))
        .build();

    return BacklogScheduledMetrics.builder()
        .expected(Indicator.builder().units(backlogExpected).build())
        .received(Indicator.builder().units(receivedBacklog).build())
        .deviation(deviation)
        .build();
  }

  private Double getDeviationPercentage(final int deviation, final int backlogExpected) {
    if (backlogExpected != 0) {
      double percentage = ((double) deviation / (double) backlogExpected) * (-1);
      return Precision.round(percentage, SCALE_DECIMAL);
    }
    return null;
  }

  private List<Photo.Group> getLastPhoto(final String warehouse, final Instant today) {

    final Photo photo = backlogGateway.getLastPhoto(
        new BacklogLastPhotoRequest(
            warehouse,
            INBOUND_WORKFLOWS,
            Set.of(),
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

  private Integer totalScheduledBacklogBetweenDate(final List<PlanningDistributionResponse> scheduledBacklogDeviated,
                                                   final Instant dateFrom,
                                                   final Instant dateTo) {
    return (int) scheduledBacklogDeviated.stream()
        .filter(scheduled -> DateUtils.isBetweenInclusive(scheduled.getDateIn().toInstant(), dateFrom, dateTo))
        .mapToLong(PlanningDistributionResponse::getTotal).sum();
  }

}
