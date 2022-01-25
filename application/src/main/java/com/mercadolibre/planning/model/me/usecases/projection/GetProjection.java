package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    protected static final int PROJECTION_DAYS = 4;
    protected static final int PROJECTION_DAYS_TO_SHOW = 1;
    protected static final int SELECTOR_DAYS_TO_SHOW = 3;
    private static final long DAYS_TO_SHOW_LOOKBACK = 0L;

    protected final PlanningModelGateway planningModelGateway;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final GetEntities getEntities;
    protected final GetProjectionSummary getProjectionSummary;

    @Override
    public Projection execute(final GetProjectionInputDto input) {

        final Instant requestDate = input.getRequestDate();
        final Instant dateFromToProject = requestDate.truncatedTo(ChronoUnit.HOURS);
        final Instant dateToToProject = dateFromToProject.plus(PROJECTION_DAYS, ChronoUnit.DAYS);

        final ZonedDateTime dateFromToShow = input.getDate() == null
                ? dateFromToProject.atZone(UTC).minusDays(getDatesToShowLookback())
                : input.getDate();

        final ZonedDateTime dateToToShow = input.getDate() == null
                ? dateFromToProject.atZone(UTC).plusDays(PROJECTION_DAYS_TO_SHOW)
                : dateFromToShow.plusDays(PROJECTION_DAYS_TO_SHOW);

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(input.getWarehouseId());

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
                    /* Note that the zone is not necessary but the getProjection method requires
                    it to no avail. */
                    dateFromToProject.atZone(UTC),
                    dateToToProject.atZone(UTC),
                    backlogsToProject,
                    config.getTimeZone().getID()
            );

            List<ProjectionResult> projectionsSla = decorateProjection(input, backlogsToProject, projectionsSlaAux);

            final List<ProjectionResult> projectionsToShow =
                    filterProjectionsInRange(dateFromToShow, dateToToShow, projectionsSla);

            return new Projection(
                    "Proyecciones",
                    getDateSelector(
                            ZonedDateTime.ofInstant(requestDate, config.getZoneId()),
                            dateFromToShow,
                            SELECTOR_DAYS_TO_SHOW),
                    new Data(
                            getWaveSuggestionTable(
                                    input.getWarehouseId(),
                                    input.getWorkflow(),
                                    config.getZoneId(),
                                    dateFromToShow
                            ),
                            getEntitiesTable(input),
                            getProjectionSummaryTable(
                                    dateFromToShow,
                                    dateToToShow,
                                    input,
                                    projectionsToShow,
                                    backlogsToShow
                            ),
                            getChart(projectionsToShow, config.getZoneId(), dateToToShow)),
                    createTabs(),
                    simulationMode);

        } catch (RuntimeException ex) {
            return new Projection(
                    "Proyecciones",
                    getDateSelector(
                            ZonedDateTime.ofInstant(requestDate, UTC),
                            dateFromToShow,
                            SELECTOR_DAYS_TO_SHOW),
                    ex.getMessage(),
                    createTabs(),
                    simulationMode
            );
        }
    }

    protected ComplexTable getEntitiesTable(final GetProjectionInputDto input) {
        return getEntities.execute(input);
    }

    protected Chart getChart(final List<ProjectionResult> projectionResult,
                             final ZoneId zoneId,
                             final ZonedDateTime dateTo) {
        return new Chart(toChartData(projectionResult, zoneId, dateTo));
    }

    private List<ProjectionResult> filterProjectionsInRange(final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<ProjectionResult> projections) {
        return projections.stream()
                .filter(p -> p.getDate().isAfter(dateFrom) && p.getDate().isBefore(dateTo))
                .collect(toList());
    }

    private List<Backlog> filterBacklogsInRange(final ZonedDateTime dateFrom,
                                                final ZonedDateTime dateTo,
                                                final List<Backlog> backlogs) {

        return backlogs.stream()
                .filter(p -> p.getDate().isAfter(dateFrom) && p.getDate().isBefore(dateTo))
                .collect(toList());
    }

    protected List<ProjectionResult> decorateProjection(final GetProjectionInputDto input,
                                                        final List<Backlog> backlogsToProject,
                                                        final List<ProjectionResult> projectionsSla) {
        return projectionsSla;
    }

    protected final boolean hasSimulatedResults(final List<ProjectionResult> projectionResults) {
        return projectionResults.stream()
                .anyMatch(p -> p.getSimulatedEndDate() != null);
    }

    protected long getDatesToShowLookback() {
        return DAYS_TO_SHOW_LOOKBACK;
    }

    protected abstract List<Tab> createTabs();

    protected abstract SimpleTable getWaveSuggestionTable(final String warehouseID,
                                                          final Workflow workflow,
                                                          final ZoneId zoneId,
                                                          final ZonedDateTime date);

    protected abstract SimpleTable getProjectionSummaryTable(final ZonedDateTime dateFromToShow,
                                                             final ZonedDateTime dateToToShow,
                                                             final GetProjectionInputDto input,
                                                             final List<ProjectionResult> projectionsToShow,
                                                             final List<Backlog> backlogsToShow);

    protected abstract List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<Backlog> backlogs,
                                                            final String timeZone);

    protected abstract List<Backlog> getBacklog(final Workflow workflow,
                                                final String warehouseId,
                                                final Instant dateFromToProject,
                                                final Instant dateToToProject,
                                                final ZoneId zoneId,
                                                final Instant requestDate);


    protected abstract List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
                                                   final ZoneId zoneId,
                                                   final ZonedDateTime dateTo);

}
