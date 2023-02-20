package com.mercadolibre.planning.model.me.usecases.monitor.deviation;

import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Map.Entry;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetDeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.sales.GetSalesByDateIn;
import com.mercadolibre.planning.model.me.services.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationActions;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationAppliedData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnit;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnitDetail;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationValues;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class GetDeviation implements UseCase<GetDeviationInput, DeviationData> {

    private static final String UNITS_DEFAULT_STRING = "%d uds.";
    private static final int DATE_OUT_LIMIT_HOURS = 96;
    public static final double HOUR_IN_MINUTES = 60;

    private final GetSalesByDateIn getSalesByDateIn;
    private final PlanningModelGateway planningModelGateway;
    private final LogisticCenterGateway logisticCenterGateway;

    @Override
    public DeviationData execute(GetDeviationInput input) {

        final List<PlanningDistributionResponse> forecast = getTotalPlannedBacklog(input);

        final Map<ZonedDateTime, List<PlanningDistributionResponse>> accumulatedByEtd =
                forecast.stream()
                        .collect(Collectors.groupingBy(PlanningDistributionResponse::getDateIn));

        final long totalPlanned = accumulatedByEtd.entrySet().stream()
                .mapToLong(this::getCurrentDateUnits)
                .sum();

        final List<Backlog> sales = getTotalSales(input);
        final int totalSales = sales.stream().mapToInt(Backlog::getQuantity).sum();
        final double totalDeviation = getDeviationPercentage(totalPlanned, totalSales);
        final DeviationAppliedData deviationAppliedData = getCurrentDeviation(
                input.getWarehouseId(),
                input.getWorkflow(),
                forecast,
                sales);

        return new DeviationData(DeviationMetric.builder()
                .deviationPercentage(Metric.builder()
                        .title("% Desviación FCST / Ventas")
                        .value(String.format("%.2f%s", totalDeviation, "%"))
                        .status(getStatusForDeviation(totalDeviation))
                        .icon(getIconForDeviation(totalDeviation))
                        .build())
                .deviationUnits(DeviationUnit.builder()
                        .title("Desviación en unidades")
                        .value(String.format(UNITS_DEFAULT_STRING,
                                Math.abs(totalPlanned - totalSales)))
                        .detail(DeviationUnitDetail.builder()
                                .forecastUnits(Metric.builder()
                                        .title("Cantidad Forecast")
                                        .value(String.format(UNITS_DEFAULT_STRING, totalPlanned))
                                        .build())
                                .currentUnits(Metric.builder()
                                        .title("Cantidad Real")
                                        .value(String.format(UNITS_DEFAULT_STRING, totalSales))
                                        .build())
                                .build())
                        .build())
                .build(),
                DeviationActions.builder()
                        .applyLabel("Ajustar forecast")
                        .unapplyLabel("Volver al forecast")
                        .appliedData(deviationAppliedData)
                        .build());
    }

    private double getDeviationPercentage(final long totalPlanned, final int totalSold) {
        return totalPlanned != 0
                ? (((double) totalSold / totalPlanned) - 1) * 100
                : 0;
    }

    private String getIconForDeviation(double totalDeviation) {
        return totalDeviation > 0 ? "arrow_up" : "arrow_down";
    }

    private String getStatusForDeviation(double totalDeviation) {
        return totalDeviation > 0 ? "warning" : null;
    }

    private List<Backlog> getTotalSales(GetDeviationInput input) {
        final ZonedDateTime dateInFrom = input.getCurrentTime().truncatedTo(DAYS);
        final ZonedDateTime dateInTo = input.getCurrentTime();
        final ZonedDateTime dateOutFrom = input.getCurrentTime();
        final ZonedDateTime dateOutTo = dateOutFrom.plusHours(DATE_OUT_LIMIT_HOURS);

        return getSalesByDateIn.execute(new GetSalesInputDto(
                input.getWorkflow(),
                input.getWarehouseId(),
                dateInFrom.withZoneSameInstant(UTC),
                dateInTo.withZoneSameInstant(UTC),
                dateOutFrom.withZoneSameInstant(UTC),
                dateOutTo.withZoneSameInstant(UTC))
        );
    }

    private List<PlanningDistributionResponse> getTotalPlannedBacklog(GetDeviationInput input) {
        final ZonedDateTime dateInFrom = input.getCurrentTime().truncatedTo(DAYS);
        final ZonedDateTime dateInTo = input.getCurrentTime();
        final ZonedDateTime dateOutFrom = input.getCurrentTime();
        final ZonedDateTime dateOutTo = dateOutFrom.plusHours(DATE_OUT_LIMIT_HOURS);

        return planningModelGateway
                .getPlanningDistribution(new PlanningDistributionRequest(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        dateInFrom.withZoneSameInstant(UTC),
                        dateInTo.withZoneSameInstant(UTC),
                        dateOutFrom.withZoneSameInstant(UTC),
                        dateOutTo.withZoneSameInstant(UTC),
                        true)
                );
    }

    private long getCurrentDateUnits(
            final Entry<ZonedDateTime,
                    List<PlanningDistributionResponse>> planningDistributions) {
        return planningDistributions.getKey().isEqual(getCurrentUtcDate())
                ? ((Double) (now(UTC).getMinute() / HOUR_IN_MINUTES
                * sumTotals(planningDistributions.getValue()))).longValue()
                : sumTotals(planningDistributions.getValue());
    }

    private long sumTotals(final List<PlanningDistributionResponse> distributionResponses) {
        return distributionResponses.stream()
                .mapToLong(PlanningDistributionResponse::getTotal)
                .sum();
    }

    private DeviationAppliedData getCurrentDeviation(final String warehouseId,
                                                     final Workflow workflow,
                                                     final List<PlanningDistributionResponse> planned,
                                                     final List<Backlog> sales) {
        final LogisticCenterConfiguration configuration =
                logisticCenterGateway.getConfiguration(warehouseId);

        DeviationAppliedData deviationAppliedData = null;

        try {
            final int denominator = 100;
            final ZonedDateTime currentDate = now().withZoneSameInstant(UTC);
            final GetDeviationResponse deviationResponse =
                    planningModelGateway.getDeviation(workflow, warehouseId, currentDate);

            if (deviationResponse != null) {
                final ZonedDateTime dateFrom = convertToTimeZone(configuration.getZoneId(),
                        deviationResponse.getDateFrom());

                final ZonedDateTime dateTo = convertToTimeZone(configuration.getZoneId(),
                        deviationResponse.getDateTo());

                final Long totalUnitsDiverted = getSalesPlanWeighted(
                        planned,
                        sales,
                        dateFrom,
                        dateTo
                );

                final String title = String.format("Se ajustó el forecast %.2f%s de %s a %s",
                        deviationResponse.getValue(), "%",
                        dateFrom.format(HOUR_MINUTES_FORMATTER),
                        dateTo.format(HOUR_MINUTES_FORMATTER)) + getDateCurrent(dateFrom, dateTo);

                final DeviationValues deviationValues = DeviationValues.builder()
                        .percentage(deviationResponse.getValue() / denominator)
                        .units(
                                (int) Math.floor(
                                        totalUnitsDiverted * deviationResponse.getValue() / denominator
                                )
                        )
                        .build();

                deviationAppliedData = new DeviationAppliedData(
                        title,
                        "info",
                        deviationValues,
                        dateFrom.withFixedOffsetZone(),
                        dateTo.withFixedOffsetZone()
                );
            }

        } catch (Exception e) {
            deviationAppliedData = null;
        }

        return deviationAppliedData;
    }

    private Long getSalesPlanWeighted(
            List<PlanningDistributionResponse> planned,
            List<Backlog> sales,
            ZonedDateTime dateFrom,
            ZonedDateTime dateTo
    ) {

        final Map<ZonedDateTime, List<PlanningDistributionResponse>> accumulatedByEtd =
                planned.stream()
                        .filter(p -> p.getDateOut().isBefore(dateTo))
                        .collect(Collectors.groupingBy(PlanningDistributionResponse::getDateIn));

        final long totalPlanned = accumulatedByEtd.entrySet().stream()
                .mapToLong(this::getCurrentDateUnits)
                .sum();

        final int totalSales = sales.stream()
                .filter(p -> p.getDate().isAfter(dateFrom))
                .filter(p -> p.getDate().isBefore(dateTo))
                .mapToInt(Backlog::getQuantity)
                .sum();

        return Math.abs(totalPlanned - totalSales);
    }

    private String getDateCurrent(final ZonedDateTime dateFrom, final ZonedDateTime dateTo) {
        final long days = DAYS.between(dateFrom.toLocalDate(), dateTo.toLocalDate());
        return days > 0 ? String.format(" (+%d).", days) : "";
    }
}
