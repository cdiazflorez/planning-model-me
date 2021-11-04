package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
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
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.util.stream.Collectors.toList;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES =
            List.of(PICKING, PACKING, PACKING_WALL);

    protected static final int PROJECTION_DAYS = 4;

    protected static final int PROJECTION_DAYS_TO_SHOW = 1;

    protected static final int SELECTOR_DAYS_TO_SHOW = 3;

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

        // Obtains the pending backlog
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
            List<ProjectionResult> projectionsSlaCpt = getProjection(input, dateFromToProject,
                    dateToToProject, backlogsToProject, config.getTimeZone().getID());

            final GetSimpleDeferralProjectionOutput deferralProjectionOutput =
                    getSimpleDeferralProjection.execute(
                            new GetProjectionInput(
                                    input.getWarehouseId(),
                                    input.getWorkflow(),
                                    input.getDate(),
                                    backlogsToProject,
                                    false,
                                    false));

            transferDeferralFlag(projectionsSlaCpt, deferralProjectionOutput.getProjections());

            final List<ProjectionResult> projectionsToShow =
                    filterProjectionsInRange(dateFromToShow, dateToToShow, projectionsSlaCpt);

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
     * Transfers the deferral flag from the deferral projections to the corresponding cpt
     * projections.
     *
     * <p>Note that the elements of the received `cptProjections` list is mutated.
     */
    private static void transferDeferralFlag(final List<ProjectionResult> cptProjections,
                                             final  List<ProjectionResult> deferralProjections) {

        final Map<Instant, ProjectionResult> deferralProjectionsByDateOut =
                deferralProjections.stream().collect(Collectors.toMap(
                    pt -> pt.getDate().toInstant(),
                    Function.identity(),
                    (pd1, pd2) ->  pd2
                ));

        for (ProjectionResult cptProjection : cptProjections) {
            final ProjectionResult deferralProjection =
                    deferralProjectionsByDateOut.get(cptProjection.getDate().toInstant());
            if (deferralProjection != null) {
                cptProjection.setDeferred(deferralProjection.isDeferred());
            } else {
                log.info("Not found cptProjection [{}] in cptDeferral", cptProjection.getDate()
                        .toInstant());
            }
        }
    }

    private List<ProjectionResult> filterProjectionsInRange(
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo,
            final List<ProjectionResult> projections) {

        return projections.stream().filter(p -> p.getDate().isAfter(dateFrom) && p.getDate()
                .isBefore(dateTo)).collect(toList());
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
