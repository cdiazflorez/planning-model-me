package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.utils.ResponseUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class GetProjectionInbound extends GetProjection {

  protected static final List<ProcessName> PROCESS_NAMES_INBOUND = List.of(CHECK_IN, PUT_AWAY);

  private static final long DAYS_TO_SHOW_LOOKBACK = 7L;

  private final GetBacklogByDateInbound getBacklogByDateInbound;

  protected GetProjectionInbound(final PlanningModelGateway planningModelGateway,
                                 final LogisticCenterGateway logisticCenterGateway,
                                 final GetEntities getEntities,
                                 final GetProjectionSummary getProjectionSummary,
                                 final GetBacklogByDateInbound getBacklogByDateInbound) {

    super(planningModelGateway, logisticCenterGateway, getEntities, getProjectionSummary);
    this.getBacklogByDateInbound = getBacklogByDateInbound;
  }

  private static List<Backlog> summarizeOverdueBacklogOf(final List<Backlog> backlogs,
                                                         final ZoneId zoneId,
                                                         final Instant now) {
    return backlogs.stream()
        .filter(backlog -> backlog.getDate().toInstant().isAfter(now) || backlog.getQuantity() > 0)
        .collect(Collectors.toMap(
            backlog -> {
              final ZonedDateTime date = backlog.getDate();
              if (date.toInstant().isAfter(now)) {
                return date;
              } else {
                final ZonedDateTime truncatedDate = convertToTimeZone(zoneId, date)
                    .truncatedTo(ChronoUnit.DAYS);

                return convertToTimeZone(ZoneId.of("Z"), truncatedDate);
              }
            },
            Backlog::getQuantity,
            Integer::sum
        ))
        .entrySet()
        .stream()
        .map(entry -> new Backlog(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(Backlog::getDate))
        .collect(toList());
  }

  @Override
  protected final List<Backlog> getBacklog(final Workflow workflow,
                                           final String warehouseId,
                                           final Instant dateFromToProject,
                                           final Instant dateToToProject,
                                           final ZoneId zoneId,
                                           final Instant requestDate) {

    final List<Backlog> backlogs = getBacklogByDateInbound.execute(
        new GetBacklogByDateDto(
            workflow,
            warehouseId,
            dateFromToProject,
            dateToToProject));

    return summarizeOverdueBacklogOf(backlogs, zoneId, requestDate);
  }

  @Override
  protected final SimpleTable getProjectionSummaryTable(final ZonedDateTime dateFromToShow,
                                                        final ZonedDateTime dateToToShow,
                                                        final GetProjectionInputDto input,
                                                        final List<ProjectionResult> projectionsToShow,
                                                        final List<Backlog> backlogsToShow) {
    return getProjectionSummary.execute(GetProjectionSummaryInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(dateFromToShow)
        .dateTo(dateToToShow)
        .projections(projectionsToShow)
        .backlogs(backlogsToShow)
        .showDeviation(false)
        .build());
  }

  @Override
  protected final SimpleTable getWaveSuggestionTable(final String warehouseID,
                                                     final Workflow workflow,
                                                     final ZoneId zoneId,
                                                     final ZonedDateTime date) {
    return null;
  }

  @Override
  protected final List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
                                              final ZoneId zoneId,
                                              final ZonedDateTime dateTo) {
    final boolean hasSimulatedResults = hasSimulatedResults(projectionResult);

    return projectionResult.stream()
        .map(projection -> {
          final ZonedDateTime projectedEndDate = hasSimulatedResults
              ? projection.getSimulatedEndDate()
              : projection.getProjectedEndDate();

          return ChartData.fromProjectionInbound(
              convertToTimeZone(zoneId, projection.getDate()),
              projectedEndDate == null
                  ? null : convertToTimeZone(zoneId, projectedEndDate),
              convertToTimeZone(zoneId, dateTo),
              projection.getRemainingQuantity(),
              new ProcessingTime(0, null),
              projection.isDeferred(),
              projection.isExpired());
        })
        .collect(toList());
  }

  @Override
  protected long getDatesToShowShift() {
    return DAYS_TO_SHOW_LOOKBACK;
  }

  @Override
  protected List<Tab> createTabs() {
    return ResponseUtils.createInboundTabs();
  }
}
