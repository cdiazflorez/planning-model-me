package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class GetProjectionSummary implements UseCase<GetProjectionSummaryInput, SimpleTable> {

    private static final int SELLING_PERIOD_HOURS = 28;

    private final GetSales getSales;

    private final PlanningModelGateway planningModelGateway;

    private final LogisticCenterGateway logisticCenterGateway;

    @Override
    public SimpleTable execute(final GetProjectionSummaryInput input) {

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        final List<Backlog> sales = getSales.execute(GetSalesInputDto.builder()
                .dateCreatedFrom(input.getDateFrom().minusHours(SELLING_PERIOD_HOURS))
                .dateCreatedTo(input.getDateFrom())
                .dateOutFrom(input.getDateFrom())
                .dateOutTo(input.getDateTo())
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .fromDS(true)
                .build()
        );

        final List<PlanningDistributionResponse> planningDistribution = planningModelGateway
                .getPlanningDistribution(PlanningDistributionRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflow(input.getWorkflow())
                        .dateInFrom(input.getDateFrom().minusHours(SELLING_PERIOD_HOURS))
                        .dateInTo(input.getDateFrom())
                        .dateOutFrom(input.getDateFrom())
                        .dateOutTo(input.getDateTo())
                        .applyDeviation(true)
                        .build());

        fixProjections(input.getProjections());

        return createProjectionDetailsTable(
                input.getBacklogs(),
                sales,
                input.getProjections(),
                config,
                planningDistribution,
                input.isShowDeviation()
        );
    }


    private void fixProjections(List<ProjectionResult> projections) {
        final ProcessingTime defaultProcessingTime = new ProcessingTime(240, "minutes");

        projections.forEach(projectionResult -> {
            if (projectionResult.getProcessingTime() == null) {
                projectionResult.setProcessingTime(defaultProcessingTime);
            }
        });
    }

    private SimpleTable createProjectionDetailsTable(
            final List<Backlog> backlogs,
            final List<Backlog> sales,
            final List<ProjectionResult> projectionResults,
            final LogisticCenterConfiguration configuration,
            final List<PlanningDistributionResponse> planningDistribution,
            final boolean showDeviation) {

        final ZoneId zoneId = configuration.getTimeZone().toZoneId();
        final boolean hasSimulatedResults = hasSimulatedResults(projectionResults);

        return new SimpleTable(
                "Resumen de Proyección",
                getProjectionDetailsTableColumns(hasSimulatedResults, showDeviation),
                getTableData(
                        backlogs,
                        sales,
                        projectionResults,
                        planningDistribution,
                        zoneId,
                        hasSimulatedResults,
                        showDeviation
                )
        );
    }

    private List<ColumnHeader> getProjectionDetailsTableColumns(
            final boolean hasSimulatedResults,
            final boolean showDeviation) {

        int columnIndex = 1;
        final List<ColumnHeader> columnHeaders = new ArrayList<>();
        columnHeaders.add(new ColumnHeader("column_" + columnIndex++, "CPT's"));
        columnHeaders.add(new ColumnHeader("column_" + columnIndex++, "Backlog actual"));

        if (showDeviation) {
            columnHeaders.add(new ColumnHeader("column_" + columnIndex++, "Desv. vs forecast"));
        }
        if (hasSimulatedResults) {
            columnHeaders.add(new ColumnHeader("column_" + columnIndex++, "Cierre actual"));
            columnHeaders.add(new ColumnHeader("column_" + columnIndex, "Cierre simulado"));
        } else {
            columnHeaders.add(new ColumnHeader("column_" + columnIndex, "Cierre proyectado"));
        }
        return columnHeaders;
    }

    private String getStyle(final ZonedDateTime cpt,
                            final ZonedDateTime projectedEndDate,
                            final long processingTime) {
        if (projectedEndDate == null || projectedEndDate.isAfter(cpt)) {
            return "danger";
        } else if (projectedEndDate.isBefore(cpt.minusMinutes(processingTime))) {
            return "none";
        } else {
            return "warning";
        }
    }

    private String getDeviation(final ZonedDateTime cpt,
                                final int backlogQuantity,
                                final List<PlanningDistributionResponse> planningDistribution) {
        final double deviation = getNumericDeviation(cpt, backlogQuantity,
                planningDistribution);
        return String.format("%.1f%s", Math.round(deviation * 100.00) / 100.00, "%");
    }

    private double getNumericDeviation(final ZonedDateTime cpt, final int backlogQuantity,
                                       final List<PlanningDistributionResponse> planning) {
        final long forecastedItemsForCpt = planning.stream()
                .filter(distribution -> cpt.isEqual(distribution.getDateOut()))
                .mapToLong(PlanningDistributionResponse::getTotal)
                .sum();

        if (forecastedItemsForCpt == 0 || backlogQuantity == 0) {
            return 0;
        }

        return (((double) backlogQuantity / forecastedItemsForCpt) - 1) * 100;
    }

    private int getBacklogQuantity(final ZonedDateTime cpt, final List<Backlog> backlogs) {
        final Optional<Backlog> cptBacklog = backlogs.stream()
                .filter(backlog -> cpt.isEqual(backlog.getDate()))
                .findFirst();

        return cptBacklog.map(Backlog::getQuantity).orElse(0);
    }

    private List<Map<String, Object>> getTableData(
            final List<Backlog> backlogs,
            final List<Backlog> sales,
            final List<ProjectionResult> projectionResults,
            final List<PlanningDistributionResponse> planning,
            final ZoneId zoneId,
            final boolean hasSimulatedResults,
            final boolean showDeviation) {

        final List<Map<String, Object>> tableData = projectionResults.stream()
                .sorted(Comparator.comparing(ProjectionResult::getDate).reversed())
                .map(projection -> getProjectionDetailsTableData(
                        backlogs,
                        sales,
                        planning,
                        zoneId,
                        projection,
                        hasSimulatedResults,
                        showDeviation)
                )
                .collect(toList());
        tableData.add(addTotalsRow(backlogs, sales, planning, showDeviation));
        return tableData;
    }

    private Map<String, Object> getProjectionDetailsTableData(
            final List<Backlog> backlogs,
            final List<Backlog> sales,
            final List<PlanningDistributionResponse> planningDistribution,
            final ZoneId zoneId,
            final ProjectionResult projection,
            final boolean hasSimulatedResults,
            final boolean showDeviation) {

        final ZonedDateTime cpt = projection.getDate();
        final ZonedDateTime projectedEndDate = projection.getProjectedEndDate();
        final ZonedDateTime simulatedEndDate = projection.getSimulatedEndDate();
        final int backlog = getBacklogQuantity(cpt, backlogs);
        final int soldItems = getBacklogQuantity(cpt, sales);

        int index = 1;
        final Map<String, Object> columns = new LinkedHashMap<>();

        columns.put("style", getStyle(
                cpt,
                projectedEndDate,
                projection.getProcessingTime().getValue()));

        columns.put("column_" + index++, convertToTimeZone(zoneId, cpt)
                .format(HOUR_MINUTES_FORMATTER));
        columns.put("column_" + index++, String.valueOf(backlog));

        if (showDeviation) {
            columns.put("column_" + index++, getDeviation(cpt, soldItems, planningDistribution));
        }

        columns.put("column_" + index++, projectedEndDate == null
                ? "Excede las 24hs"
                : convertToTimeZone(zoneId, projectedEndDate).format(HOUR_MINUTES_FORMATTER));

        if (hasSimulatedResults) {
            columns.put("column_" + index, simulatedEndDate == null
                    ? "Excede las 24hs"
                    : convertToTimeZone(zoneId, simulatedEndDate).format(HOUR_MINUTES_FORMATTER));
        }

        if (projection.isDeferred()) {
            columns.put("is_deferred", true);
        }

        return columns;
    }

    private Map<String, Object> addTotalsRow(final List<Backlog> backlogs,
                                             final List<Backlog> realSales,
                                             final List<PlanningDistributionResponse> planning,
                                             final boolean showDeviation) {
        int index = 1;
        final Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("style", "none");
        columns.put("column_" + index++, "Total");
        columns.put("column_" + index++, String.valueOf(calculateTotalFromBacklog(backlogs)));

        if (showDeviation) {
            columns.put("column_" + index++, calculateDeviationTotal(
                    calculateTotalForecast(planning),
                    calculateTotalFromBacklog(realSales)));
        }
        columns.put("column_" + index++, "");
        columns.put("column_" + index, "");

        return columns;
    }

    private long calculateTotalForecast(final List<PlanningDistributionResponse> planning) {
        return planning.stream().mapToLong(PlanningDistributionResponse::getTotal).sum();
    }

    private int calculateTotalFromBacklog(List<Backlog> backlogs) {
        return backlogs.stream().mapToInt(Backlog::getQuantity).sum();
    }

    private String calculateDeviationTotal(
            final long totalForecast, final int totalRealSales) {
        if (totalForecast == 0) {
            return "0%";
        }
        final double totalDeviation = (((double) totalRealSales / totalForecast) - 1) * 100;
        return String.format("%.2f%s", totalDeviation, "%");
    }

    private boolean hasSimulatedResults(List<ProjectionResult> projectionResults) {
        return projectionResults.stream().anyMatch(p -> p.getSimulatedEndDate() != null);
    }
}
