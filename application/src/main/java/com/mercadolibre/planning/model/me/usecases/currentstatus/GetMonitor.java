package com.mercadolibre.planning.model.me.usecases.currentstatus;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation.DeviationMetric;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation.DeviationUnit;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation.DeviationUnitDetail;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.utils.DateUtils;

import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;

@Named
@AllArgsConstructor
public class GetMonitor implements UseCase<GetMonitorInput, Monitor> {
    
    private static final String ICON_ATTRIBUTE = "icon";

    private static final String STATUS_ATTRIBUTE = "status";

    private static final String UNITS_DEFAULT_STRING = "%d uds.";

    private static final int SELLING_PERIOD_HOURS = 28;

    final BacklogGatewayProvider backlogGatewayProvider;
    
    protected final LogisticCenterGateway logisticCenterGateway;
    
    private GetSales getSales;

    private PlanningModelGateway planningModelGateway;

    @Override
    public Monitor execute(GetMonitorInput input) {
        final List<MonitorData> monitorDataList = getMonitorData(input);
        final String currentTime = getCurrentTime(input);
        return Monitor.builder()
                .title("Modelo de Priorización")
                .subtitle1("Estado Actual")
                .subtitle2("Última actualización: Hoy - " + currentTime)
                .monitorData(monitorDataList)
                .build();
    }

    private String getCurrentTime(GetMonitorInput input) {
        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime currentDate = convertToTimeZone(config.getZoneId(), now);
        final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("HH:mm");
        return currentDate.format(formatter2);
    }

    private List<MonitorData> getMonitorData(GetMonitorInput input) {
        final DeviationData deviationData = getDeviationData(input);
        final CurrentStatusData currentStatusData = getCurrentStatusData(input);
        return List.of(deviationData, currentStatusData);
    }

    private DeviationData getDeviationData(GetMonitorInput input) {
        final List<PlanningDistributionResponse> plannedBacklogs = getPlannedBacklog(input);

        final List<Backlog> realSales = getSales(input);
        
        final long totalPlanned = plannedBacklogs.stream()
                .mapToLong(planned -> planned.getTotal()).sum();
        
        final int totalSales = realSales.stream()
                .mapToInt(realSale -> realSale.getQuantity()).sum();
        
        final long difference = Math.abs(totalPlanned - totalSales);
        
        final double totalDeviation = getTotalDeviation(totalPlanned, totalSales);
        
        return buildDeviationData(totalDeviation, totalPlanned, totalSales, difference);
    }

    private double getTotalDeviation(final long totalBacklog,
            final int totalSales) {
        final double totalDeviation = (((double) totalSales / totalBacklog) - 1) * 100;
        return Math.round(totalDeviation * 100.00) / 100.00;
    }

    private DeviationData buildDeviationData(final double totalDeviation, final long totalPlanned,
            final int totalSales, final long difference) {
        DeviationData deviationData = new DeviationData(DeviationMetric.builder()
                .deviationPercentage(Metric.builder()
                        .title("% Desviación FCST / Ventas")
                        .value(String.format("%.1f%s",totalDeviation,"%"))
                        .status(getStyleForDeviation(totalDeviation,STATUS_ATTRIBUTE))
                        .icon(getStyleForDeviation(totalDeviation, ICON_ATTRIBUTE))
                        .build())
                    .deviationUnits(DeviationUnit.builder()
                        .title("Desviación en unidades")
                        .value(String.format(UNITS_DEFAULT_STRING, difference))
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
        return deviationData;
    }

    private String getStyleForDeviation(double totalDeviation, String attribute) {
        if (Objects.equals(attribute, ICON_ATTRIBUTE)) {
            return totalDeviation > 0 ? "arrow_up" : "arrow_down";
        } else if (Objects.equals(attribute, STATUS_ATTRIBUTE)) {
            return totalDeviation > 0 ? "warning" : null;
        }
        return null;
    }

    private List<Backlog> getSales(GetMonitorInput input) {
        return getSales.execute(new GetSalesInputDto(
                input.getWorkflow(),
                input.getWarehouseId(),
                DateUtils.getCurrentUtcDate().minusHours(SELLING_PERIOD_HOURS))
        );
    }

    private List<PlanningDistributionResponse> getPlannedBacklog(GetMonitorInput input) {
        return  planningModelGateway
                .getPlanningDistribution(new PlanningDistributionRequest(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getDateFrom(),
                        input.getDateFrom(),
                        input.getDateTo()));
        
    }
    
    private CurrentStatusData getCurrentStatusData(GetMonitorInput input) {
        final ArrayList<Process> processes = new ArrayList<>();
        addMetricBacklog(input, processes);
        addThroughputMetric();
        addProductivityMetric();
        return CurrentStatusData.builder().processes(processes).build();
    }

    private void addMetricBacklog(GetMonitorInput input, ArrayList<Process> processes) {
        final String status = STATUS_ATTRIBUTE;
        List<Map<String, String>> statuses = List.of(
                Map.of(status, OUTBOUND_PLANNING.getStatus()),
                Map.of(status, PACKING.getStatus())
        );
        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()));
        List<ProcessBacklog> processBacklogs = backlogGateway.getBacklog(statuses,
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo());
        final ProcessBacklog pickingBacklog = backlogGateway.getUnitBacklog(PICKING.getStatus(),
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo());
        final ProcessBacklog wallInBacklog = backlogGateway.getUnitBacklog(WALL_IN.getStatus(),
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo());
        processBacklogs.addAll(Arrays.asList(pickingBacklog, wallInBacklog));

        processBacklogs.forEach(processBacklog ->
                addProcessIfExist(processes, processBacklog)
        );
        completeProcess(processes);
    }

    private void addThroughputMetric() {
        //TODO Get and add throughput metric
    }

    private void addProductivityMetric() {
        //TODO Get and add productivity metric
    }

    private void addProcessIfExist(ArrayList<Process> processes,
                                   final ProcessBacklog processBacklog) {
        getProcessBy(processBacklog, OUTBOUND_PLANNING)
                .ifPresentOrElse(processes::add,
                        () -> getProcessBy(processBacklog, PICKING)
                                .ifPresentOrElse(processes::add,
                                        () -> getProcessBy(processBacklog, PACKING)
                                                .ifPresentOrElse(processes::add,
                                                        () -> getProcessBy(processBacklog, WALL_IN)
                                                                .ifPresent(processes::add)
                                                )
                                )
            );
    }

    private Optional<Process> getProcessBy(final ProcessBacklog processBacklog,
                                           final ProcessInfo processInfo) {
        if (processBacklog.getProcess().equalsIgnoreCase(processInfo.getStatus())) {
            final String quantity = NumberFormat.getNumberInstance(Locale.GERMAN)
                            .format(processBacklog.getQuantity());
            final Metric metric = Metric.builder()
                    .type(BACKLOG.getType())
                    .title(BACKLOG.getTitle())
                    .subtitle(processInfo.getSubtitle())
                    .value(quantity + " uds.")
                    .build();
            final Process process = Process.builder()
                    .title(processInfo.getTitle())
                    .metrics(List.of(metric))
                    .build();
            return Optional.of(process);
        }
        return Optional.empty();
    }

    private void completeProcess(ArrayList<Process> processes) {
        List<ProcessInfo> processList = List.of(OUTBOUND_PLANNING, PACKING);
        processList.stream().filter(t -> processes.stream()
                .noneMatch(current -> current.getTitle().equalsIgnoreCase(t.getTitle()))
        ).forEach(noneMatchProcess -> {
                    final Metric metric = Metric.builder()
                            .type(BACKLOG.getType())
                            .title(BACKLOG.getTitle())
                            .subtitle(noneMatchProcess.getSubtitle())
                            .value("0 uds.")
                            .build();
                    processes.add(Process.builder()
                            .metrics(List.of(metric))
                            .title(noneMatchProcess.getTitle())
                            .build()
                    );
        }
        );
    }

}
