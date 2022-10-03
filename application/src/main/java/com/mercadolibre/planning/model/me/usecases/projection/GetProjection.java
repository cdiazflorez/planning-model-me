package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ResultData;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionDataInput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements template method for SLA projection and simulation use cases.
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, PlanningView> {
  protected static final int PROJECTION_DAYS = 4;

  protected static final int PROJECTION_DAYS_TO_SHOW = 1;

  protected static final int SELECTOR_DAYS_TO_SHOW = 3;

  private static final long DAYS_TO_SHOW_LOOKBACK = 0L;

  private static final Boolean PROJECTIONS_VERSION = true;

  private static final int SELLING_PERIOD_HOURS = 28;

  protected final GetSales getSales;

  protected final PlanningModelGateway planningModelGateway;

  protected final LogisticCenterGateway logisticCenterGateway;

  protected final GetEntities getEntities;

  @Override
  public PlanningView execute(final GetProjectionInputDto input) {
    final Instant requestDate = input.getRequestDate();

    final Instant dateFromToProject = requestDate.truncatedTo(ChronoUnit.HOURS);

    final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(input.getWarehouseId());

    final boolean isFirstDate = isFirstDate(input.getDate(), requestDate);

    final ZonedDateTime dateFromToShow = isFirstDate
        ? ZonedDateTime.ofInstant(requestDate, UTC).minusDays(getDatesToShowShift())
        : input.getDate();

    final ZonedDateTime dateToToShow = isFirstDate
        ? dateFromToProject.atZone(UTC).plusDays(PROJECTION_DAYS_TO_SHOW)
        : dateFromToShow.plusDays(PROJECTION_DAYS_TO_SHOW);

    final Instant dateToToProject = dateToToShow.plusHours(1).toInstant();

    // Obtains the pending backlog
    final List<Backlog> backlogsToProject = getBacklog(
        input.getWorkflow(),
        input.getWarehouseId(),
        dateFromToProject,
        dateToToProject,
        config.getZoneId(),
        input.getRequestDate());

    final List<Backlog> backlogsToShow = filterBacklogsInRange(dateFromToShow, dateToToShow, backlogsToProject);

    try {
      List<ProjectionResult> projectionsSlaAux = getProjection(
          input,
          /* Note that the zone is not necessary but the getProjection method requires it to no avail. */
          dateFromToProject.atZone(UTC),
          dateToToProject.atZone(UTC),
          backlogsToProject,
          config
      );

      final List<ProjectionResult> projectionsSla =
          decorateProjection(input, backlogsToProject, projectionsSlaAux);

      final List<ProjectionResult> projectionsToShow =
          filterProjectionsInRange(dateFromToShow, dateToToShow, projectionsSla);

      return PlanningView.builder()
          .isNewVersion(PROJECTIONS_VERSION)
          .currentDate(now().withZoneSameInstant(UTC).truncatedTo(ChronoUnit.SECONDS))
          .dateSelector(getDateSelector(
              ZonedDateTime.ofInstant(requestDate, config.getZoneId()),
              dateFromToShow,
              SELECTOR_DAYS_TO_SHOW))
          .data(new ResultData(
              getWaveSuggestionTable(
                  input.getWarehouseId(),
                  input.getWorkflow(),
                  config.getZoneId(),
                  dateFromToShow
              ),
              getEntitiesTable(input),
              getProjectionData(input,
                  dateFromToShow,
                  dateToToShow,
                  backlogsToShow,
                  projectionsToShow)))
          .build();

    } catch (RuntimeException ex) {
      log.error(ex.getMessage(), ex);
      return PlanningView.builder()
          .isNewVersion(PROJECTIONS_VERSION)
          .currentDate(now().withZoneSameInstant(UTC).truncatedTo(ChronoUnit.SECONDS))
          .dateSelector(getDateSelector(
              ZonedDateTime.ofInstant(requestDate, config.getZoneId()),
              dateFromToShow,
              SELECTOR_DAYS_TO_SHOW))
          .emptyStateMessage(ex.getMessage())
          .build();
    }
  }

  private boolean isFirstDate(final ZonedDateTime inputDate, final Instant requestDate) {
    if (inputDate == null) {
      return true;
    } else {
      final Instant selectedDate = inputDate.truncatedTo(ChronoUnit.HOURS).toInstant();
      final Instant currentDate = requestDate.truncatedTo(ChronoUnit.HOURS);
      return selectedDate.equals(currentDate);
    }
  }

  protected ComplexTable getEntitiesTable(final GetProjectionInputDto input) {
    return getEntities.execute(input);
  }

  private List<ProjectionResult> filterProjectionsInRange(final ZonedDateTime dateFrom,
                                                          final ZonedDateTime dateTo,
                                                          final List<ProjectionResult> projections) {
    return projections.stream()
        .filter(p -> (p.getDate().isAfter(dateFrom) || p.getDate().isEqual(dateFrom))
            && p.getDate().isBefore(dateTo))
        .collect(toList());
  }

  private List<Backlog> filterBacklogsInRange(final ZonedDateTime dateFrom,
                                              final ZonedDateTime dateTo,
                                              final List<Backlog> backlogs) {

    return backlogs.stream()
        .filter(p -> (p.getDate().isAfter(dateFrom) || p.getDate().isEqual(dateFrom)) && p.getDate().isBefore(dateTo))
        .collect(toList());
  }

  protected List<ProjectionResult> decorateProjection(final GetProjectionInputDto input,
                                                      final List<Backlog> backlogsToProject,
                                                      final List<ProjectionResult> projectionsSla) {
    return projectionsSla;
  }

  private List<Projection> getProjectionData(final GetProjectionInputDto input,
                                             final ZonedDateTime dateFrom,
                                             final ZonedDateTime dateTo,
                                             final List<Backlog> backlogs,
                                             final List<ProjectionResult> projection) {

    final List<Backlog> sales = getRealBacklog(input.getWarehouseId(), input.getWorkflow(), dateFrom, dateTo);

    final List<PlanningDistributionResponse> planningDistribution = getForecastedBacklog(input.getWarehouseId(),
        input.getWorkflow(), dateFrom, dateTo);

    return ProjectionDataMapper.map(GetProjectionDataInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .sales(sales)
        .planningDistribution(planningDistribution)
        .projections(projection)
        .backlogs(backlogs)
        .showDeviation(true)
        .build());
  }

  protected List<PlanningDistributionResponse> getForecastedBacklog(final String warehouseId,
                                                                    final Workflow workflow,
                                                                    final ZonedDateTime dateFrom,
                                                                    final ZonedDateTime dateTo) {

    return planningModelGateway.getPlanningDistribution(PlanningDistributionRequest.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateInFrom(dateFrom.minusHours(SELLING_PERIOD_HOURS))
        .dateInTo(dateFrom)
        .dateOutFrom(dateFrom)
        .dateOutTo(dateTo)
        .applyDeviation(true)
        .build());
  }

  private List<Backlog> getRealBacklog(final String warehouseId,
                                       final Workflow workflow,
                                       final ZonedDateTime dateFrom,
                                       final ZonedDateTime dateTo) {
    return getSales.execute(GetSalesInputDto.builder()
        .dateCreatedFrom(dateFrom.minusHours(SELLING_PERIOD_HOURS))
        .dateCreatedTo(dateFrom)
        .dateOutFrom(dateFrom)
        .dateOutTo(dateTo)
        .workflow(workflow)
        .warehouseId(warehouseId)
        .fromDS(true)
        .build());
  }

  protected long getDatesToShowShift() {
    return DAYS_TO_SHOW_LOOKBACK;
  }

  protected abstract SimpleTable getWaveSuggestionTable(String warehouseID,
                                                        Workflow workflow,
                                                        ZoneId zoneId,
                                                        ZonedDateTime date);

  protected abstract List<ProjectionResult> getProjection(GetProjectionInputDto input,
                                                          ZonedDateTime dateFrom,
                                                          ZonedDateTime dateTo,
                                                          List<Backlog> backlogs,
                                                          LogisticCenterConfiguration config);

  protected abstract List<Backlog> getBacklog(Workflow workflow,
                                              String warehouseId,
                                              Instant dateFromToProject,
                                              Instant dateToToProject,
                                              ZoneId zoneId,
                                              Instant requestDate);
}
