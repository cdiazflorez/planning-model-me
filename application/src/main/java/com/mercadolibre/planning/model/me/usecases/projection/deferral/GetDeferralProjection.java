package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Named;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
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

    private static final List<ProcessName> PROCESS_NAMES = List.of(PICKING, PACKING, PACKING_WALL);

    private static final int DAYS_TO_SHOW = 2;

    private static final int HOURS_TO_SHOW = 25;

    private static final int DEFERRAL_DAYS = 3;

    private static final int PROJECTION_DAYS_TO_SHOW = 1;

    private final LogisticCenterGateway logisticCenterGateway;

    private final PlanningModelGateway planningModelGateway;

    private final GetBacklogByDate getBacklog;

    private final GetProjectionSummary getProjectionSummary;

    @Override
    public Projection execute(final GetProjectionInput input) {

        final ZonedDateTime dateFromToProject = getCurrentUtcDate();
        final ZonedDateTime dateToToProject = dateFromToProject.plusDays(DEFERRAL_DAYS);

        final ZonedDateTime dateFromToShow = input.getDate() == null
                ? dateFromToProject : input.getDate();

        final ZonedDateTime dateToToShow = dateFromToShow.plusDays(PROJECTION_DAYS_TO_SHOW);

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getLogisticCenterId());

        final List<Backlog> backlogsToProject = getBacklog.execute(
                new GetBacklogByDateDto(input.getWorkflow(),
                        input.getLogisticCenterId(), dateFromToProject, dateToToProject));

        final List<Backlog> backlogsToShow =
                filterBacklogsInRange(dateFromToShow, dateToToShow, backlogsToProject);

        try {
            final List<ProjectionResult> projections =
                    getProjection(input, dateFromToProject, dateToToProject, backlogsToProject);

            setDeferrals(projections, config.getZoneId());

            final List<ProjectionResult> projectionsToShow =
                    filterProjectionsInRange(dateFromToShow, dateToToShow, projections);

            return new Projection(
                    "Proyección",
                    getDateSelector(dateFromToShow, DAYS_TO_SHOW),
                    null,
                    new Data(null,
                            getThroughput(config, input, dateFromToShow, dateToToShow),
                            getProjectionSummary(input,
                                    dateFromToShow,
                                    dateToToShow,
                                    backlogsToShow,
                                    projectionsToShow),
                            new Chart(toChartData(projectionsToShow, config, dateToToShow))),
                    createTabs(),
                    null);

        } catch (RuntimeException ex) {
            return new Projection("Proyección",
                    getDateSelector(dateFromToShow, DAYS_TO_SHOW),
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
                    p.isDeferred()
            ));
        }

        return chart.stream().sorted(Comparator.comparing(ChartData::getCpt)).collect(toList());
    }

    private List<ProjectionResult> getProjection(final GetProjectionInput input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs) {

        final List<ProjectionResult> projection = planningModelGateway.runDeferralProjection(
                ProjectionRequest.builder()
                        .warehouseId(input.getLogisticCenterId())
                        .workflow(input.getWorkflow())
                        .processName(PROCESS_NAMES)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .backlog(backlogs)
                        .build());

        return projection.stream()
                .sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate()))
                .collect(toList());
    }

    private void setDeferrals(final List<ProjectionResult> projections,
                              final ZoneId zoneId) {

        boolean isDeferred = false;

        for (ProjectionResult p : projections) {
            final ZonedDateTime projectedEndDate = p.getProjectedEndDate();
            final ZonedDateTime cutOffDate =
                    p.getDate().minusMinutes(p.getProcessingTime().getValue());

            if (projectedEndDate == null || projectedEndDate.isAfter(cutOffDate)) {
                isDeferred = true;
            }

            p.setDeferred(isDeferred);
        }
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

        final  List<Entity> throughput = planningModelGateway.getEntities(
                EntityRequest.builder()
                        .warehouseId(input.getLogisticCenterId())
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
}
