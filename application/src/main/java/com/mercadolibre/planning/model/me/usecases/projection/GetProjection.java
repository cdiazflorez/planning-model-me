package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES =
            List.of(PICKING, PACKING, PACKING_WALL);

    protected static final int PROJECTION_DAYS = 4;

    protected static final int PROJECTION_DAYS_TO_SHOW = 1;

    protected static final int SELECTOR_DAYS_TO_SHOW = 3;

    protected static final int PROCESSING_TIME_DEFAULT = 240;

    protected final PlanningModelGateway planningModelGateway;

    protected final LogisticCenterGateway logisticCenterGateway;

    protected final GetWaveSuggestion getWaveSuggestion;

    protected final GetEntities getEntities;

    protected final GetProjectionSummary getProjectionSummary;

    protected final GetBacklogByDate getBacklog;

    protected final GetSimpleDeferralProjection getSimpleDeferralProjection;

    @Override
    public Projection execute(final GetProjectionInputDto input) {

        final ZonedDateTime dateFromToProject = getCurrentUtcDate();
        final ZonedDateTime dateToToProject = dateFromToProject.plusDays(PROJECTION_DAYS);

        final ZonedDateTime dateFromToShow = input.getDate() == null
                ? dateFromToProject : input.getDate();
        final ZonedDateTime dateToToShow = dateFromToShow.plusDays(PROJECTION_DAYS_TO_SHOW);

        final List<Backlog> backlogsToProject = getBacklog.execute(
                new GetBacklogByDateDto(
                        input.getWorkflow(),
                        input.getWarehouseId(),
                        dateFromToProject,
                        dateToToProject)
        );

        final List<Backlog> backlogsToShow =
                filterBacklogsInRange(dateFromToShow, dateToToShow, backlogsToProject);

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        try {
            List<ProjectionResult> projectionsCpt = getProjection(input, dateFromToProject,
                    dateToToProject, backlogsToProject, config.getTimeZone().getID());

            final GetSimpleDeferralProjectionOutput deferralsCpt =
                    getSimpleDeferralProjection.execute(
                            new GetProjectionInput(
                                    input.getWarehouseId(),
                                    input.getWorkflow(),
                                    input.getDate(),
                                    backlogsToProject,
                                    false));

            setDeferralCascade(projectionsCpt, dateFromToProject, deferralsCpt);

            final List<ProjectionResult> projectionsToShow =
                    filterProjectionsInRange(dateFromToShow, dateToToShow, projectionsCpt);

            return new Projection(
                    "Proyecciones",
                    getDateSelector(dateFromToShow, SELECTOR_DAYS_TO_SHOW),
                    null,
                    new Data(getWaveSuggestion.execute(GetWaveSuggestionInputDto.builder()
                            .zoneId(config.getZoneId())
                            .warehouseId(input.getWarehouseId())
                            .workflow(input.getWorkflow())
                            .date(dateFromToShow)
                            .build()),
                            getEntities.execute(input),
                            getProjectionSummary.execute(GetProjectionSummaryInput.builder()
                                    .workflow(input.getWorkflow())
                                    .warehouseId(input.getWarehouseId())
                                    .dateFrom(dateFromToShow)
                                    .dateTo(dateToToShow)
                                    .projections(projectionsToShow)
                                    .backlogs(backlogsToShow)
                                    .showDeviation(true)
                                    .build()),
                            new Chart(toChartData(projectionsToShow, config.getZoneId(),
                                    dateToToShow))),
                    createTabs(),
                    simulationMode);

        } catch (RuntimeException ex) {
            return new Projection("Proyecciones",
                    getDateSelector(dateFromToShow, SELECTOR_DAYS_TO_SHOW),
                    ex.getMessage(),
                    null,
                    createTabs(),
                    simulationMode
            );
        }
    }

    private boolean hasSimulatedResults(final List<ProjectionResult> projectionResults) {
        return projectionResults.stream().anyMatch(p -> p.getSimulatedEndDate() != null);
    }

    private List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
                                        final ZoneId zoneId,
                                        final ZonedDateTime dateTo) {
        final boolean hasSimulatedResults = hasSimulatedResults(projectionResult);

        return projectionResult.stream()
                .map(projection -> {
                    final ZonedDateTime projectedEndDate = hasSimulatedResults
                            ? projection.getSimulatedEndDate()
                            : projection.getProjectedEndDate();

                    return ChartData.fromProjection(
                            convertToTimeZone(zoneId, projection.getDate()),
                            projectedEndDate == null
                                    ? null : convertToTimeZone(zoneId, projectedEndDate),
                            convertToTimeZone(zoneId, dateTo),
                            projection.getRemainingQuantity(),
                            projection.getProcessingTime(),
                            projection.isDeferred());
                })
                .collect(toList());
    }

    /**
     * This method sets the deferral in cascade, from two lists, the projected cpts and the deferred
     * cpts, the projected cpts are iterated
     * and we consult the list of deferred cpts when finding the first deferred cpt, the following
     * are set in the property isDeferral = true.
     */

    private void setDeferralCascade(final List<ProjectionResult> projectionsCpt,
                                    final ZonedDateTime currentDate,
                                    final GetSimpleDeferralProjectionOutput deferralsCpt) {

        final Map<ZonedDateTime, ProjectionResult> deferralsCptMap = deferralsCpt
                .getProjections().stream().collect(Collectors.toMap(ProjectionResult::getDate,
                        Function.identity(), (pd1, pd2) -> pd2));

        boolean isDeferred = false;

        for (ProjectionResult p : projectionsCpt.stream()
                .sorted(comparing(ProjectionResult::getDate, reverseOrder()))
                .collect(toList())) {

            final boolean cptFound = deferralsCptMap.containsKey(p.getDate());

            final ZonedDateTime cutOffDate =
                    p.getDate().minusMinutes(p.getProcessingTime().getValue());

            if (cptFound && deferralsCptMap.get(p.getDate()).isDeferred()) {
                isDeferred = true;
            }
            p.setDeferred(isDeferred && cutOffDate.isAfter(currentDate));

            p.setProcessingTime(
                    new ProcessingTime(
                            PROCESSING_TIME_DEFAULT,
                            MINUTES.getName()));
        }
    }

    private List<ProjectionResult> filterProjectionsInRange(
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo,
            final List<ProjectionResult> projections) {

        return projections.stream().filter(p -> p.getDate().isAfter(dateFrom)
                        && p.getDate().isBefore(dateTo))
                .collect(toList());
    }

    private List<Backlog> filterBacklogsInRange(
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo,
            final List<Backlog> backlogs) {

        return backlogs.stream().filter(p -> p.getDate().isAfter(dateFrom)
                        && p.getDate().isBefore(dateTo))
                .collect(toList());
    }

    protected abstract List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<Backlog> backlogs,
                                                            final String timeZone);
}
