package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createColumnHeaders;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createData;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createOutboundTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.clock.RequestClockGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class GeneralDeferralProjection implements UseCase<GetProjectionInput, Projection> {

    private static final int DEFERRAL_DAYS_TO_PROJECT = 3;

    private static final int DEFERRAL_DAYS_TO_SHOW = 1;

    private static final int SELECTOR_DAYS_TO_SHOW = 2;

    private static final int HOURS_TO_SHOW = 25;

    private static final Map<MagnitudeType, Map<String, List<String>>> FILTER_CAP_MAX = Map.of(
            HEADCOUNT, Map.of(
                    PROCESSING_TYPE.toJson(),
                    List.of(MAX_CAPACITY.getName())
            )
    );

    private static final List<String> CAP5_TO_PACK_STATUSES = List.of("pending", "to_route",
            "to_pick", "picked", "to_sort", "sorted", "to_group", "grouping", "grouped", "to_pack");

    private final PlanningModelGateway planningModelGateway;

    private final GetProjectionSummary getProjectionSummary;

    private final DeferralBaseProjection getSimpleDeferralProjection;

    private final BacklogApiGateway backlogGateway;

    private final RequestClockGateway requestClockGateway;

    @Override
    public Projection execute(final GetProjectionInput input) {

        final ZonedDateTime requestDateTime = ZonedDateTime.ofInstant(requestClockGateway.now(), UTC);
        final ZonedDateTime dateFromToProject = requestDateTime.truncatedTo(ChronoUnit.HOURS);
        final ZonedDateTime dateToToProject = dateFromToProject.plusDays(DEFERRAL_DAYS_TO_PROJECT);
        final Instant requestDate = requestDateTime.truncatedTo(ChronoUnit.MINUTES).toInstant();

        try {

            final boolean isSameDayHour = isSameDayHour(input.getDate(), requestDate);
            final ZonedDateTime dateFromToShow = isSameDayHour
                    ? ZonedDateTime.ofInstant(requestDate, UTC)
                    : input.getDate();

            final ZonedDateTime dateToToShow = dateFromToShow.plusDays(DEFERRAL_DAYS_TO_SHOW);

            final List<Backlog> backlogsToProject = getBacklog(
                    input.getLogisticCenterId(),
                    dateFromToProject,
                    dateToToProject);

            final GetSimpleDeferralProjectionOutput deferralBaseOutput =
                    getSimpleDeferralProjection.execute(
                            new GetProjectionInput(
                                    input.getLogisticCenterId(),
                                    input.getWorkflow(),
                                    input.getDate(),
                                    backlogsToProject,
                                    input.isWantToSimulate21(),
                                    input.getSimulations()));

            final List<Backlog> backlogsToShow = filterBacklogsInRange(
                    dateFromToShow,
                    dateToToShow,
                    backlogsToProject);

            final List<ProjectionResult> projectionsToShow =
                    filterProjectionsInRange(dateFromToShow, dateToToShow,
                            deferralBaseOutput.getProjections());

            return new Projection(
                    "Proyección",
                    getDateSelector(dateFromToProject, dateFromToShow, SELECTOR_DAYS_TO_SHOW),
                    new Data(null,
                            getThroughput(deferralBaseOutput.getConfiguration(),
                                    input,
                                    dateFromToShow.truncatedTo(ChronoUnit.HOURS),
                                    dateToToShow),
                            getProjectionSummary(input,
                                    dateFromToShow,
                                    dateToToShow,
                                    backlogsToShow,
                                    projectionsToShow),
                            new Chart(toChartData(projectionsToShow,
                                    deferralBaseOutput.getConfiguration(),
                                    dateToToShow))),
                    createOutboundTabs(),
                    simulationMode);

        } catch (RuntimeException ex) {
            return new Projection("Proyección",
                    getDateSelector(
                            requestDateTime,
                            input.getDate() != null
                                    ? input.getDate()
                                    : requestDateTime,
                            SELECTOR_DAYS_TO_SHOW),
                    ex.getMessage(),
                    createOutboundTabs(),
                    null);
        }
    }

    private boolean isSameDayHour(final ZonedDateTime inputDate, final Instant requestDate) {
        if (inputDate == null) {
            return true;
        } else {
            final Instant selectedDate = inputDate.truncatedTo(ChronoUnit.HOURS).toInstant();
            final Instant currentDate = requestDate.truncatedTo(ChronoUnit.HOURS);
            return selectedDate.equals(currentDate);
        }
    }

    private List<ChartData> toChartData(final List<ProjectionResult> projections,
                                        final LogisticCenterConfiguration config,
                                        final ZonedDateTime dateTo) {

        final List<ChartData> chart = new ArrayList<>();
        final ZoneId zoneId = config.getZoneId();

        for (ProjectionResult p : projections) {

            chart.add(ChartData.fromProjection(
                    convertToTimeZone(zoneId, p.getDate()),
                    p.getProjectedEndDate() == null
                            ? null : convertToTimeZone(zoneId, p.getProjectedEndDate()),
                    convertToTimeZone(zoneId, dateTo),
                    p.getRemainingQuantity(),
                    p.getProcessingTime(),
                    p.isDeferred(),
                    p.isExpired()
            ));
        }

        return chart.stream().sorted(Comparator.comparing(ChartData::getCpt)).collect(toList());
    }

    private SimpleTable getProjectionSummary(final GetProjectionInput input,
                                             final ZonedDateTime dateFrom,
                                             final ZonedDateTime dateTo,
                                             final List<Backlog> backlogs,
                                             final List<ProjectionResult> projection) {

        if (projection.isEmpty()) {
            return new SimpleTable(
                    "Resumen de Proyección",
                    new ArrayList<>(List.of(
                            new ColumnHeader("column_1", "CPT's", null),
                            new ColumnHeader("column_2", "Backlog actual", null),
                            new ColumnHeader("column_3", "Cierre proyectado", null)
                    )),
                    Collections.emptyList()
            );
        } else {
            return getProjectionSummary.execute(GetProjectionSummaryInput.builder()
                    .workflow(input.getWorkflow())
                    .warehouseId(input.getLogisticCenterId())
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .projections(projection)
                    .backlogs(backlogs)
                    .showDeviation(false)
                    .build());
        }
    }

    private ComplexTable getThroughput(final LogisticCenterConfiguration config,
                                       final GetProjectionInput input,
                                       final ZonedDateTime dateFrom,
                                       final ZonedDateTime dateTo) {

        final Map<MagnitudeType, List<MagnitudePhoto>> entities = planningModelGateway.searchTrajectories(
                SearchTrajectoriesRequest.builder()
                        .warehouseId(input.getLogisticCenterId())
                        .workflow(FBM_WMS_OUTBOUND)
                        .entityTypes(List.of(HEADCOUNT, THROUGHPUT))
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .processName(List.of(GLOBAL, PACKING, PACKING_WALL))
                        .entityFilters(FILTER_CAP_MAX)
                        .source(Source.SIMULATION)
                        .build()
        );

        final List<MagnitudePhoto> maxCapacity = entities.get(HEADCOUNT);

        final List<MagnitudePhoto> throughput = entities.get(THROUGHPUT);

        changeThroughputBySimulations(input.getSimulations(), maxCapacity);

        final List<ColumnHeader> headers = createColumnHeaders(
                convertToTimeZone(config.getZoneId(), dateFrom), HOURS_TO_SHOW);

        return new ComplexTable(
                headers,
                List.of(createData(config, THROUGHPUT, maxCapacity.stream()
                        .map(entity -> EntityRow.fromEntity(entity, validateCapacity(entity, throughput)))
                        .collect(toList()), headers)
                ),
                action,
                ""
        );
    }

    private boolean validateCapacity(MagnitudePhoto capacity, List<MagnitudePhoto> throughput) {
        return capacity.getValue() >= throughput.stream()
                .filter(magnitudePhoto -> magnitudePhoto.getDate().isEqual(capacity.getDate()))
                .mapToInt(MagnitudePhoto::getValue)
                .sum();
    }

    private void changeThroughputBySimulations(List<Simulation> simulations, List<MagnitudePhoto> throughput) {
        if (simulations != null && !simulations.isEmpty()) {
            Map<ZonedDateTime, Integer> simulationDates = simulations.stream()
                    .filter(simulation -> simulation.getProcessName().equals(GLOBAL))
                    .map(Simulation::getEntities)
                    .flatMap(Collection::stream)
                    .filter(simulationEntity -> simulationEntity.getType().equals(THROUGHPUT))
                    .map(SimulationEntity::getValues)
                    .flatMap(Collection::stream)
                    .collect(toMap(
                            quantityByDate -> quantityByDate.getDate().withZoneSameInstant(ZoneOffset.UTC), QuantityByDate::getQuantity
                    ));

            throughput.forEach(magnitudePhoto -> {
                Integer tphSimulated = simulationDates.get(magnitudePhoto.getDate());
                if (tphSimulated != null) {
                    magnitudePhoto.setValue(tphSimulated);
                }
            });

        }

    }

    private List<ProjectionResult> filterProjectionsInRange(
            final ZonedDateTime dateFrom,
            final ZonedDateTime dateTo,
            final List<ProjectionResult> projections) {

        return projections.stream().filter(p ->
                        (p.getDate().isEqual(dateFrom) || p.getDate().isAfter(dateFrom))
                                && (p.getDate().isEqual(dateTo) || p.getDate().isBefore(dateTo)))
                .collect(toList());
    }

    private List<Backlog> filterBacklogsInRange(final ZonedDateTime dateFrom,
                                                final ZonedDateTime dateTo,
                                                final List<Backlog> backlogs) {

        return backlogs.stream().filter(p ->
                        (p.getDate().isEqual(dateFrom) || p.getDate().isAfter(dateFrom))
                                && p.getDate().isBefore(dateTo))
                .collect(toList());
    }

    private List<Backlog> getBacklog(final String logisticCenterId,
                                     final ZonedDateTime dateFrom,
                                     final ZonedDateTime dateTo) {

        final String groupingKey = BacklogGrouper.DATE_OUT.getName();

        var backlogBySla = backlogGateway.getCurrentBacklog(
                logisticCenterId,
                List.of("outbound-orders"),
                CAP5_TO_PACK_STATUSES,
                dateFrom.toInstant(),
                dateTo.toInstant(),
                List.of(groupingKey));

        return backlogBySla.stream()
                .map(backlog -> new Backlog(
                        ZonedDateTime.parse(backlog.getKeys().get(groupingKey)),
                        backlog.getTotal()))
                .collect(Collectors.toList());
    }
}
