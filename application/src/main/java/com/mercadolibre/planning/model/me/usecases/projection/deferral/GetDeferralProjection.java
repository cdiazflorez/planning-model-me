package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createColumnHeaders;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createData;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createOutboundTabs;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.simulationMode;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetDeferralProjection implements UseCase<GetProjectionInput, Projection> {

    private static final int DEFERRAL_DAYS_TO_PROJECT = 3;

    private static final int DEFERRAL_DAYS_TO_SHOW = 1;

    private static final int SELECTOR_DAYS_TO_SHOW = 2;

    private static final int HOURS_TO_SHOW = 25;

    private static final List<String> CAP5_TO_PACK_STATUSES = List.of("pending", "to_route",
            "to_pick", "picked", "to_sort", "sorted", "to_group", "grouping", "grouped", "to_pack");

    private final PlanningModelGateway planningModelGateway;

    private final GetProjectionSummary getProjectionSummary;

    private final GetSimpleDeferralProjection getSimpleDeferralProjection;

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
                                    input.isWantToSimulate21()));

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

        final List<MagnitudePhoto> maxCapacity = planningModelGateway.getTrajectories(
                TrajectoriesRequest.builder()
                        .warehouseId(input.getLogisticCenterId())
                        .workflow(input.getWorkflow())
                        .entityType(HEADCOUNT)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .processName(List.of(GLOBAL))
                        .processingType(List.of(MAX_CAPACITY))
                        .build()
        );

        final List<MagnitudePhoto> throughput = planningModelGateway.getTrajectories(
                TrajectoriesRequest.builder()
                        .warehouseId(input.getLogisticCenterId())
                        .workflow(input.getWorkflow())
                        .entityType(THROUGHPUT)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .processName(List.of(PACKING, PACKING_WALL))
                        .processingType(List.of(ProcessingType.THROUGHPUT))
                        .build()
        );

        final Map<ZonedDateTime, Integer> throughputOutboundByHours = throughput.stream()
                .collect(Collectors.toMap(
                        MagnitudePhoto::getDate,
                        MagnitudePhoto::getValue,
                        Integer::sum));

        final List<ColumnHeader> headers = createColumnHeaders(
                convertToTimeZone(config.getZoneId(), dateFrom), HOURS_TO_SHOW);

        return new ComplexTable(
                headers,
                List.of(createData(config, THROUGHPUT, maxCapacity.stream()
                        .map(entity -> EntityRow.fromEntity(entity, validateCapacity(entity, throughputOutboundByHours)))
                        .collect(toList()), headers)
                ),
                action,
                ""
        );
    }

    private boolean validateCapacity(MagnitudePhoto capacity, Map<ZonedDateTime, Integer> throughputOutboundByHours) {
        boolean valid = capacity.getValue() >= throughputOutboundByHours.getOrDefault(capacity.getDate(), 0);

        return !capacity.getProcessName().equals(ProcessName.GLOBAL) || valid;
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
