package com.mercadolibre.planning.model.me.usecases.monitor.deviation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.deviation.GetDeviationInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnit;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnitDetail;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.ZoneOffset.UTC;

@Named
@AllArgsConstructor
public class GetDeviation implements UseCase<GetDeviationInput, DeviationData> {

    private static final String UNITS_DEFAULT_STRING = "%d uds.";
    private static final int DATE_OUT_LIMIT_HOURS = 24;

    private final GetSales getSales;
    private final PlanningModelGateway planningModelGateway;

    @Override
    public DeviationData execute(GetDeviationInput input) {
        final long totalPlanned = getTotalPlannedBacklog(input);

        final int totalSales = getTotalSales(input);
        final double totalDeviation = getDeviationPercentage(totalPlanned, totalSales);

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

    private int getTotalSales(GetDeviationInput input) {
        final ZonedDateTime dateInFrom = input.getCurrentTime().truncatedTo(ChronoUnit.DAYS);
        final ZonedDateTime dateInTo = input.getCurrentTime();
        final ZonedDateTime dateOutFrom = input.getCurrentTime();
        final ZonedDateTime dateOutTo = dateOutFrom.plusHours(DATE_OUT_LIMIT_HOURS);

        final List<Backlog> sales = getSales.execute(new GetSalesInputDto(
                input.getWorkflow(),
                input.getWarehouseId(),
                dateInFrom.withZoneSameInstant(UTC),
                dateInTo.withZoneSameInstant(UTC),
                dateOutFrom.withZoneSameInstant(UTC),
                dateOutTo.withZoneSameInstant(UTC))
        );

        return sales.stream().mapToInt(Backlog::getQuantity).sum();
    }

    private long getTotalPlannedBacklog(GetDeviationInput input) {
        final ZonedDateTime dateInFrom = input.getCurrentTime().truncatedTo(ChronoUnit.DAYS);
        final ZonedDateTime dateInTo = input.getCurrentTime();
        final ZonedDateTime dateOutFrom = input.getCurrentTime();
        final ZonedDateTime dateOutTo = dateOutFrom.plusHours(DATE_OUT_LIMIT_HOURS);

        List<PlanningDistributionResponse> forecast = planningModelGateway
                .getPlanningDistribution(new PlanningDistributionRequest(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        dateInFrom.withZoneSameInstant(UTC),
                        dateInTo.withZoneSameInstant(UTC),
                        dateOutFrom.withZoneSameInstant(UTC),
                        dateOutTo.withZoneSameInstant(UTC))
        );

        return forecast.stream().mapToLong(PlanningDistributionResponse::getTotal).sum();
    }
}
