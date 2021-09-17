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
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GetProjection implements UseCase<GetProjectionInputDto, Projection> {

    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES =
            List.of(PICKING, PACKING, PACKING_WALL);

    protected static final int DAYS_TO_SHOW = 3;

    protected final PlanningModelGateway planningModelGateway;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final GetWaveSuggestion getWaveSuggestion;
    protected final GetEntities getEntities;
    protected final GetProjectionSummary getProjectionSummary;
    protected final GetBacklogByDate getBacklog;

    @Override
    public Projection execute(final GetProjectionInputDto input) {
        final ZonedDateTime utcDateFrom = input.getDate() == null
                ? getCurrentUtcDate() : input.getDate();
        final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

        final List<Backlog> backlogs = getBacklog.execute(
                new GetBacklogByDateDto(input.getWorkflow(),
                        input.getWarehouseId(),
                        utcDateFrom, utcDateTo)
        );

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        try {
            final List<ProjectionResult> projections = getProjection(
                    input, utcDateFrom, utcDateTo, backlogs);

            return new Projection(
                    "Proyecciones",
                    getDateSelector(utcDateFrom, DAYS_TO_SHOW),
                    null,
                    new Data(getWaveSuggestion.execute(GetWaveSuggestionInputDto.builder()
                            .zoneId(config.getZoneId())
                            .warehouseId(input.getWarehouseId())
                            .workflow(input.getWorkflow())
                            .date(utcDateFrom)
                            .build()),
                            getEntities.execute(input),
                            getProjectionSummary.execute(GetProjectionSummaryInput.builder()
                                    .workflow(input.getWorkflow())
                                    .warehouseId(input.getWarehouseId())
                                    .dateFrom(utcDateFrom)
                                    .dateTo(utcDateTo)
                                    .projections(projections)
                                    .backlogs(backlogs)
                                    .showDeviation(true)
                                    .build()),
                            new Chart(toChartData(projections, config.getZoneId(), utcDateTo))),
                    createTabs(),
                    simulationMode);

        } catch (RuntimeException ex) {
            return new Projection("Proyecciones",
                    getDateSelector(utcDateFrom, DAYS_TO_SHOW),
                    ex.getMessage(),
                    null,
                    createTabs(),
                    simulationMode
            );
        }
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
                            projection.getRemainingQuantity(),
                            projection.getProcessingTime(),
                            projection.isDeferred());
                })
                .collect(Collectors.toList());
    }

    protected abstract List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo,
                                                            final List<Backlog> backlogs);
}
