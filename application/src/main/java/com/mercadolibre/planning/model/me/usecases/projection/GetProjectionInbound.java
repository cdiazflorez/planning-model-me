package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionDataInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class GetProjectionInbound extends GetProjection {
  protected static final List<ProcessName> PROCESS_NAMES_INBOUND = List.of(CHECK_IN, PUT_AWAY);

  private static final long DAYS_TO_SHOW_LOOKBACK = 30L;

  private final GetBacklogByDateInbound getBacklogByDateInbound;

  protected GetProjectionInbound(final PlanningModelGateway planningModelGateway,
                                 final LogisticCenterGateway logisticCenterGateway,
                                 final GetEntities getEntities,
                                 final GetBacklogByDateInbound getBacklogByDateInbound,
                                 final GetSales getSales) {

    super(getSales, planningModelGateway, logisticCenterGateway, getEntities);
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
                final ZonedDateTime truncatedDate = now.atZone(zoneId).truncatedTo(ChronoUnit.DAYS);

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
  protected long getDatesToShowShift() {
    return DAYS_TO_SHOW_LOOKBACK;
  }

  @Override
  protected List<Projection> getProjectionData(
      final GetProjectionInputDto input,
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo,
      final List<Backlog> backlogs,
      final List<ProjectionResult> projection) {

    final List<Backlog> sales = getRealBacklog(input.getWarehouseId(), input.getWorkflow(), dateFrom, dateTo);

    return ProjectionDataMapper.map(GetProjectionDataInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .sales(sales)
        .planningDistribution(Collections.emptyList())
        .projections(projection)
        .backlogs(backlogs)
        .showDeviation(true)
        .build());
  }

}
