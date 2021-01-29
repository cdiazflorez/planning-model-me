package com.mercadolibre.planning.model.me.usecases.monitor.deviation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnit;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnitDetail;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Named
@AllArgsConstructor
public class GetDeviation implements UseCase<GetMonitorInput, DeviationData> {
    private static final String UNITS_DEFAULT_STRING = "%d uds.";
    private static final int SELLING_PERIOD_HOURS = 28;
    private final GetSales getSales;
    private final PlanningModelGateway planningModelGateway;

    @Override
    public DeviationData execute(GetMonitorInput input) {
        final ZonedDateTime current = DateUtils.getCurrentUtcDate();
        final long totalPlanned = getTotalPlannedBacklog(input, current);
        final int totalSales = getTotalSales(input, current);
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

    private int getTotalSales(GetMonitorInput input, ZonedDateTime current) {
        final List<Backlog> sales = getSales.execute(new GetSalesInputDto(
                input.getWorkflow(),
                input.getWarehouseId(),
                current.truncatedTo(ChronoUnit.DAYS),
                current,
                current,
                current.plusHours(24))
        );
        return sales.stream().mapToInt(Backlog::getQuantity).sum();
    }

    private long getTotalPlannedBacklog(
            GetMonitorInput input, ZonedDateTime current) {
        List<PlanningDistributionResponse> forecast = planningModelGateway
                .getPlanningDistribution(new PlanningDistributionRequest(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        current.truncatedTo(ChronoUnit.DAYS),
                        current,
                        current,
                        current.plusHours(24))
        );
        return forecast.stream()
                .mapToLong(PlanningDistributionResponse::getTotal).sum();
    }
}
