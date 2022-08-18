package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.utils.DateUtils.DATE_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.DATE_HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetProjectionSummary implements UseCase<GetProjectionSummaryInput, SimpleTable> {

  private static final int SELLING_PERIOD_HOURS = 28;
  private static final String COLUMN_TAG = "column_";

  private final GetSales getSales;

  private final PlanningModelGateway planningModelGateway;

  private final LogisticCenterGateway logisticCenterGateway;

  @Override
  public SimpleTable execute(final GetProjectionSummaryInput input) {
    final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
        input.getWarehouseId());

    final List<Backlog> sales = getRealBacklog(input);
    final List<PlanningDistributionResponse> planningDistribution = getForecastedBacklog(input);

    fixProjections(input.getProjections());

    return createProjectionDetailsTable(
        input.getBacklogs(),
        sales,
        input.getProjections(),
        config,
        planningDistribution,
        input.isShowDeviation(),
        input.getWorkflow()
    );
  }

  private List<Backlog> getRealBacklog(final GetProjectionSummaryInput input) {
    return input.isShowDeviation()
        ? getSales.execute(GetSalesInputDto.builder()
        .dateCreatedFrom(input.getDateFrom().minusHours(SELLING_PERIOD_HOURS))
        .dateCreatedTo(input.getDateFrom())
        .dateOutFrom(input.getDateFrom())
        .dateOutTo(input.getDateTo())
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .fromDS(true)
        .build())
        : emptyList();
  }

  private List<PlanningDistributionResponse> getForecastedBacklog(final GetProjectionSummaryInput input) {
    return input.isShowDeviation()
        ? planningModelGateway.getPlanningDistribution(PlanningDistributionRequest.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .dateInFrom(input.getDateFrom().minusHours(SELLING_PERIOD_HOURS))
        .dateInTo(input.getDateFrom())
        .dateOutFrom(input.getDateFrom())
        .dateOutTo(input.getDateTo())
        .applyDeviation(true)
        .build())
        : emptyList();
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
      final boolean showDeviation,
      final Workflow workflow) {

    final ZoneId zoneId = configuration.getTimeZone().toZoneId();
    final boolean hasSimulatedResults = hasSimulatedResults(projectionResults);

    return new SimpleTable(
        "Resumen de Proyección",
        getProjectionDetailsTableColumns(hasSimulatedResults, showDeviation, workflow),
        getTableData(
            backlogs,
            sales,
            projectionResults,
            planningDistribution,
            zoneId,
            hasSimulatedResults,
            showDeviation,
            workflow
        )
    );
  }

  private List<ColumnHeader> getProjectionDetailsTableColumns(
      final boolean hasSimulatedResults,
      final boolean showDeviation,
      final Workflow workflow) {

    int columnIndex = 1;
    final List<ColumnHeader> columnHeaders = new ArrayList<>();
    columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++,
        workflow == Workflow.FBM_WMS_INBOUND ? "SLA" : "CPT's"));
    columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++, "Backlog actual"));

    if (showDeviation) {
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++, "Desv. vs forecast"));
    }
    if (hasSimulatedResults) {
      if (workflow.equals(Workflow.FBM_WMS_INBOUND)) {
        columnIndex++;
      }
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++, "Cierre actual"));
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex, "Cierre simulado"));
    } else {
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex, "Cierre proyectado"));
    }

    if (workflow.equals(Workflow.FBM_WMS_OUTBOUND)) {
      columnIndex++;
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++, "Hora de diferimiento"));
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++, "Tipo de diferimiento"));
      columnHeaders.add(new ColumnHeader(COLUMN_TAG + columnIndex++, "Unidades a diferir"));
    }
    return columnHeaders;
  }

  private String getStyle(final ZonedDateTime cpt,
                          final ZonedDateTime projectedEndDate,
                          final long processingTime,
                          final Workflow workflow) {
    if (projectedEndDate == null || projectedEndDate.isAfter(cpt)) {
      return "danger";
    } else if (projectedEndDate.isBefore(cpt.minusMinutes(processingTime))) {
      return "none";
    } else if (workflow.equals(Workflow.FBM_WMS_OUTBOUND)) {
      return "warning";
    } else {
      return "none";
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
      final boolean showDeviation,
      final Workflow workflow) {

    final List<Map<String, Object>> tableData = projectionResults.stream()
        .sorted(Comparator.comparing(ProjectionResult::getDate).reversed())
        .map(projection -> getProjectionDetailsTableData(
            backlogs,
            sales,
            planning,
            zoneId,
            projection,
            hasSimulatedResults,
            showDeviation,
            workflow)
        )
        .collect(toList());
    tableData.add(addTotalsRow(backlogs, sales, planning, showDeviation, workflow));
    return tableData;
  }

  private Map<String, Object> getProjectionDetailsTableData(
      final List<Backlog> backlogs,
      final List<Backlog> sales,
      final List<PlanningDistributionResponse> planningDistribution,
      final ZoneId zoneId,
      final ProjectionResult projection,
      final boolean hasSimulatedResults,
      final boolean showDeviation,
      final Workflow workflow) {

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
        projection.getProcessingTime().getValue(),
        workflow));

    columns.put(COLUMN_TAG + index++, convertToTimeZone(zoneId, cpt)
        .format(projection.isExpired() ? DATE_FORMATTER : HOUR_MINUTES_FORMATTER));

    columns.put(COLUMN_TAG + index++, String.valueOf(backlog));

    if (showDeviation) {
      columns.put(COLUMN_TAG + index++, getDeviation(cpt, soldItems, planningDistribution));
    }

    if (hasSimulatedResults && workflow.equals(Workflow.FBM_WMS_INBOUND)) {
      //index is adding here, so that send correct column number in simulation inbound
      index++;
    }

    columns.put(COLUMN_TAG + index++, getProjectedEndDateLabel(projectedEndDate, backlog, zoneId));

    if (hasSimulatedResults) {
      columns.put(COLUMN_TAG + index, simulatedEndDate == null
          ? "Excede las 24hs"
          : convertToTimeZone(zoneId, simulatedEndDate)
          .format(DATE_HOUR_MINUTES_FORMATTER));
    }

    if (projection.isDeferred()) {
      columns.put("is_deferred", true);
    }

    if (workflow.equals(Workflow.FBM_WMS_OUTBOUND)) {
      columns.put(COLUMN_TAG + index++, projection.getDeferredAt());
      columns.put(COLUMN_TAG + index++, projection.getDeferralStatus());
      columns.put(COLUMN_TAG + index++, projection.getDeferredUnits() == 0
          ? null
          : projection.getDeferredUnits());
    }

    return columns;
  }

  private String getProjectedEndDateLabel(final ZonedDateTime projectedEndDate,
                                          final int backlog,
                                          final ZoneId zoneId) {
    if (projectedEndDate == null) {
      return "Excede las 24hs";
    } else {
      final ZonedDateTime hourStart = now().withZoneSameInstant(UTC).truncatedTo(HOURS);
      if (backlog == 0 && projectedEndDate.isEqual(hourStart)) {
        return "-";
      } else {
        return convertToTimeZone(zoneId, projectedEndDate)
            .format(DATE_HOUR_MINUTES_FORMATTER);
      }
    }
  }

  private Map<String, Object> addTotalsRow(final List<Backlog> backlogs,
                                           final List<Backlog> realSales,
                                           final List<PlanningDistributionResponse> planning,
                                           final boolean showDeviation,
                                           final Workflow workflow) {
    int index = 1;
    final Map<String, Object> columns = new LinkedHashMap<>();
    columns.put("style", "none");
    columns.put(COLUMN_TAG + index++, "Total");
    columns.put(COLUMN_TAG + index++, String.valueOf(calculateTotalFromBacklog(backlogs)));


    if (showDeviation) {
      columns.put(COLUMN_TAG + index++, calculateDeviationTotal(
          calculateTotalForecast(planning),
          calculateTotalFromBacklog(realSales)));

      columns.put(COLUMN_TAG + index++, "");
      columns.put(COLUMN_TAG + index, "");
    }

    if (workflow.equals(Workflow.FBM_WMS_INBOUND)) {
      columns.put(COLUMN_TAG + index++, "");
    }

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
