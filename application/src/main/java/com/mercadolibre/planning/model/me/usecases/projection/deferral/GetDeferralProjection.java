package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.utils.TestLogisticCenterMapper;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Named;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getDateSelector;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.action;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createColumnHeaders;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createData;
import static com.mercadolibre.planning.model.me.utils.ResponseUtils.createTabs;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class GetDeferralProjection implements UseCase<GetProjectionInput, Projection> {

    private static final int DEFERRAL_DAYS_TO_PROJECT = 3;

    private static final int DEFERRAL_DAYS_TO_SHOW = 1;

    private static final int SELECTOR_DAYS_TO_SHOW = 2;

    private static final int HOURS_TO_SHOW = 25;

    private static final List<String> CAP5_TO_PACK_STATUSES = List.of("pending", "planning",
            "to_pick", "picking", "sorting", "to_group", "grouping", "grouped", "to_pack");

    private static final List<String> CAP5_RTW_STATUSES = List.of("pending");

    private final PlanningModelGateway planningModelGateway;

    private final GetProjectionSummary getProjectionSummary;

    private final GetSimpleDeferralProjection getSimpleDeferralProjection;

    private final BacklogGateway backlogGateway;

    @Override
    public Projection execute(final GetProjectionInput input) {

        try {
            final ZonedDateTime dateFromToProject = getCurrentUtcDate();
            final ZonedDateTime dateToToProject = dateFromToProject
                    .plusDays(DEFERRAL_DAYS_TO_PROJECT);

            final ZonedDateTime dateFromToShow = input.getDate() == null
                    ? dateFromToProject : input.getDate();

            final ZonedDateTime dateToToShow = dateFromToShow.plusDays(DEFERRAL_DAYS_TO_SHOW);

            final List<String> backlogStatuses = input.is21Cap5Logic() || input.isWantToSimulate21()
                    ? CAP5_TO_PACK_STATUSES : CAP5_RTW_STATUSES;

            final List<Backlog> backlogsToProject = backlogGateway.getBacklog(
                    input.getLogisticCenterId(),
                    dateFromToProject,
                    dateToToProject,
                    backlogStatuses,
                    List.of("etd"));

            final GetSimpleDeferralProjectionOutput deferralBaseOutput =
                    getSimpleDeferralProjection.execute(
                            new GetProjectionInput(
                                    input.getLogisticCenterId(),
                                    input.getWorkflow(),
                                    input.getDate(),
                                    backlogsToProject,
                                    input.is21Cap5Logic(),
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
                    getDateSelector(dateFromToShow, SELECTOR_DAYS_TO_SHOW),
                    null,
                    new Data(null,
                            getThroughput(deferralBaseOutput.getConfiguration(),
                                    input,
                                    dateFromToShow,
                                    dateToToShow),
                            getProjectionSummary(input,
                                    dateFromToShow,
                                    dateToToShow,
                                    backlogsToShow,
                                    projectionsToShow),
                            new Chart(toChartData(projectionsToShow,
                                    deferralBaseOutput.getConfiguration(),
                                    dateToToShow))),
                    createTabs(),
                    null);

        } catch (RuntimeException ex) {
            return new Projection("Proyección",
                    getDateSelector(input.getDate() != null
                            ? input.getDate()
                            : getCurrentUtcDate(), SELECTOR_DAYS_TO_SHOW),
                    ex.getMessage(),
                    null,
                    createTabs(),
                    null);
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

        if (CollectionUtils.isEmpty(projection)) {
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

        final  List<MagnitudePhoto> throughput = planningModelGateway.getTrajectories(
                TrajectoriesRequest.builder()
                        .warehouseId(getCap5LogisticCenterId(input))
                        .workflow(input.getWorkflow())
                        .entityType(HEADCOUNT)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .processName(List.of(GLOBAL))
                        .processingType(List.of(MAX_CAPACITY))
                        .build()
        );

        final List<ColumnHeader> headers = createColumnHeaders(
                convertToTimeZone(config.getZoneId(), dateFrom), HOURS_TO_SHOW);

        return new ComplexTable(
                headers,
                List.of(createData(config, THROUGHPUT, throughput.stream()
                        .map(EntityRow::fromEntity).collect(toList()), headers)
                ),
                action,
                ""
        );
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
                                && (p.getDate().isEqual(dateTo) || p.getDate().isBefore(dateTo)))
                .collect(toList());
    }

    // This method is only for test in MLM and should be deleted in the future
    public static String getCap5LogisticCenterId(final GetProjectionInput input) {
        final String logisticCenterId = input.getLogisticCenterId();

        return input.isWantToSimulate21()
                ? TestLogisticCenterMapper.toFakeLogisticCenter(logisticCenterId)
                : logisticCenterId;
    }
}
