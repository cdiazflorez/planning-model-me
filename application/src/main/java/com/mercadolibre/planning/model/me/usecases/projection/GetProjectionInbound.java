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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createInboundTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;

public abstract class GetProjectionInbound extends GetProjection {

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final long DAYS_TO_SHOW_LOOKBACK = 7L;

    final GetBacklogByDateInbound getBacklogByDateInbound;

    protected static final List<ProcessName> PROCESS_NAMES_INBOUND = List.of(PUT_AWAY);

    protected GetProjectionInbound(final PlanningModelGateway planningModelGateway,
                                   final LogisticCenterGateway logisticCenterGateway,
                                   final GetWaveSuggestion getWaveSuggestion,
                                   final GetEntities getEntities,
                                   final GetProjectionSummary getProjectionSummary,
                                   final GetBacklogByDateInbound getBacklogByDateInbound) {

        super(planningModelGateway, logisticCenterGateway, getWaveSuggestion, getEntities, getProjectionSummary);
        this.getBacklogByDateInbound = getBacklogByDateInbound;
    }


    @Override
    protected List<Backlog> getBacklog(Workflow workflow, String warehouseId, Instant dateFromToProject,
                                       Instant dateToToProject) {

        return getBacklogByDateInbound.execute(
                new GetBacklogByDateDto(
                        workflow,
                        warehouseId,
                        dateFromToProject,
                        dateToToProject));
    }

    @Override
    protected Projection getProjectionReturn(final ZonedDateTime dateFromToShow,
                                             final ZonedDateTime dateToToShow,
                                             final LogisticCenterConfiguration config,
                                             final GetProjectionInputDto input,
                                             final List<ProjectionResult> projectionsToShow,
                                             final List<Backlog> backlogsToShow) {

        List<ChartData> chartData = toChartData(projectionsToShow, config.getZoneId(),
                dateToToShow)
                .stream().map(dataChart -> {
                    if (dataChart.getIsExpired()) {
                        return ChartData.builder()
                                .title(dataChart.getTitle())
                                .cpt(convertToTimeZone(config.getZoneId(),
                                        ZonedDateTime.now().withZoneSameInstant(UTC)
                                                .truncatedTo(ChronoUnit.HOURS)).withFixedOffsetZone()
                                        .format(DATE_FORMATTER))
                                .projectedEndTime(dataChart.getProjectedEndTime())
                                .processingTime(dataChart.getProcessingTime())
                                .tooltip(dataChart.getTooltip())
                                .isDeferred(dataChart.getIsDeferred())
                                .isExpired(dataChart.getIsExpired())
                                .build();
                    }

                    return dataChart;
                }).collect(Collectors.toList());

        return new Projection(
                "Proyecciones", getDateSelector(dateFromToShow, SELECTOR_DAYS_TO_SHOW),
                null,
                //Outbound GetWaveSuggestion
                new Data(null,
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
                        new Chart(chartData)),
                createInboundTabs(),
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

    protected long getDatesToShowLookback() {
        return DAYS_TO_SHOW_LOOKBACK;
    }

}
