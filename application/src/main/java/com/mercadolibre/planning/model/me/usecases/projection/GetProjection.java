package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    private static final String PROCESSING_TIME = "processing_time";

    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES =
            List.of(PICKING, PACKING, PACKING_WALL);

    protected final PlanningModelGateway planningModelGateway;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final GetWaveSuggestion getWaveSuggestion;
    protected final GetEntities getEntities;
    protected final GetProjectionSummary getProjectionSummary;
    protected final GetBacklog getBacklog;

    @Override
    public Projection execute(final GetProjectionInputDto input) {
        final ZonedDateTime utcDateFrom = getCurrentUtcDate();
        final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

        final List<Backlog> backlogs = getBacklog.execute(
                new GetBacklogInputDto(input.getWorkflow(), input.getWarehouseId())
        );

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        final List<ProjectionResult> projections = getProjection(
                input, utcDateFrom, utcDateTo, backlogs);

        final ProcessingTime processingTime = createProcessingTimeObject(
                planningModelGateway.getConfiguration(
                        ConfigurationRequest.builder()
                                .warehouseId(input.getWarehouseId())
                                .key(PROCESSING_TIME)
                                .build()));

        return new Projection(
                "Proyecciones",
                getWaveSuggestion.execute(GetWaveSuggestionInputDto.builder()
                        .zoneId(config.getZoneId())
                        .warehouseId(input.getWarehouseId())
                        .workflow(input.getWorkflow())
                        .build()
                ),
                getEntities.execute(input),
                getProjectionSummary.execute(GetProjectionSummaryInput.builder()
                        .workflow(input.getWorkflow())
                        .warehouseId(input.getWarehouseId())
                        .dateFrom(utcDateFrom)
                        .dateTo(utcDateTo)
                        .processingTime(processingTime)
                        .projections(projections)
                        .backlogs(backlogs)
                        .build()),
                new Chart(
                        processingTime,
                        toChartData(projections, config.getZoneId(), utcDateTo)
                ),
                createTabs(),
                simulationMode
        );
    }

    private ProcessingTime createProcessingTimeObject(
            Optional<ConfigurationResponse> processingTimeConfiguration) {
        return processingTimeConfiguration
                .map(configurationResponse -> new ProcessingTime(configurationResponse.getValue(),
                        configurationResponse.getMetricUnit().getName()))
                .orElseGet(() -> new ProcessingTime(60, MINUTES.getName()));
    }

    private boolean hasSimulatedResults(List<ProjectionResult> projectionResults) {
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
                            convertToTimeZone(zoneId, projectedEndDate == null
                                    ? dateTo : projectedEndDate),
                            convertToTimeZone(zoneId, dateTo),
                            projection.getRemainingQuantity());
                })
                .collect(Collectors.toList());
    }

    protected abstract List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<Backlog> backlogs);
}
