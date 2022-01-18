package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class GetProjectionOutbound extends GetProjection {

    final GetSimpleDeferralProjection getSimpleDeferralProjection;
    final BacklogApiGateway backlogGateway;

    protected static final List<ProcessName> PROCESS_NAMES_OUTBOUND = List.of(PICKING, PACKING, PACKING_WALL);

    protected GetProjectionOutbound(final PlanningModelGateway planningModelGateway,
                                    final LogisticCenterGateway logisticCenterGateway,
                                    final GetWaveSuggestion getWaveSuggestion,
                                    final GetEntities getEntities,
                                    final GetProjectionSummary getProjectionSummary,
                                    final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                    final BacklogApiGateway backlogGateway) {

        super(planningModelGateway, logisticCenterGateway, getWaveSuggestion, getEntities, getProjectionSummary);

        this.getSimpleDeferralProjection = getSimpleDeferralProjection;
        this.backlogGateway = backlogGateway;
    }

    @Override
    protected List<ProjectionResult> decorateProjection(final GetProjectionInputDto input,
                                                        final List<Backlog> backlogsToProject,
                                                        final List<ProjectionResult> projectionsSla) {

        final GetSimpleDeferralProjectionOutput deferralProjectionOutput =
                getSimpleDeferralProjection.execute(
                        new GetProjectionInput(
                                input.getWarehouseId(),
                                input.getWorkflow(),
                                input.getDate(),
                                backlogsToProject,
                                false));

        return transferDeferralFlag(projectionsSla, deferralProjectionOutput.getProjections());
    }

    @Override
    protected List<Backlog> getBacklog(final Workflow workflow,
                                       final String warehouseId,
                                       final Instant dateFromToProject,
                                       final Instant dateToToProject) {

        final String groupingKey = BacklogGrouper.DATE_OUT.getName();

        var backlogBySla = backlogGateway.getCurrentBacklog(
                warehouseId,
                List.of("outbound-orders"),
                List.of("pending"),
                dateFromToProject,
                dateToToProject,
                List.of(groupingKey));


        return backlogBySla.stream()
                .map(backlog -> new Backlog(
                        ZonedDateTime.parse(backlog.getKeys().get(groupingKey)),
                        backlog.getTotal()))
                .collect(Collectors.toList());
    }

    @Override
    protected Projection getProjectionReturn(final ZonedDateTime dateFromToShow,
                                             final ZonedDateTime dateToToShow,
                                             final LogisticCenterConfiguration config,
                                             final GetProjectionInputDto input,
                                             final List<ProjectionResult> projectionsToShow,
                                             final List<Backlog> backlogsToShow) {
        return new Projection(
                "Proyecciones", getDateSelector(dateFromToShow, SELECTOR_DAYS_TO_SHOW),
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
    }

    @Override
    public List<ChartData> toChartData(final List<ProjectionResult> projectionResult,
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
                            projection.isDeferred(),
                            projection.isExpired());
                })
                .collect(toList());
    }

    private List<ProjectionResult> transferDeferralFlag(final List<ProjectionResult> slaProjections,
                                                        final List<ProjectionResult> deferralProjections) {

        final Map<Instant, ProjectionResult> deferralProjectionsByDateOut =
                deferralProjections.stream().collect(Collectors.toMap(
                        pt -> pt.getDate().toInstant(),
                        Function.identity(),
                        (pd1, pd2) -> pd2
                ));

        final List<ProjectionResult> newSlaProjections = new ArrayList<>(slaProjections);

        for (ProjectionResult slaProjection : newSlaProjections) {
            final ProjectionResult deferralProjection =
                    deferralProjectionsByDateOut.get(slaProjection.getDate().toInstant());
            if (deferralProjection != null) {
                slaProjection.setDeferred(deferralProjection.isDeferred());
            } else {
                log.info("Not found cptProjection [{}] in cptDeferral", slaProjection.getDate()
                        .toInstant());
            }
        }

        return newSlaProjections;
    }
}
