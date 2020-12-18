package com.mercadolibre.planning.model.me.usecases.currentstatus;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;

@Named
@AllArgsConstructor
public class GetMonitor implements UseCase<GetMonitorInput, Monitor> {

    final BacklogGatewayProvider backlogGatewayProvider;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final AnalyticsGateway analyticsClient;

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
        final DeviationData deviationData = getDeviationData();
        final CurrentStatusData currentStatusData = getCurrentStatusData(input);
        return List.of(deviationData, currentStatusData);
    }

    private DeviationData getDeviationData() {
        return new DeviationData();
    }

    private CurrentStatusData getCurrentStatusData(GetMonitorInput input) {
        final ArrayList<Process> processes = new ArrayList<>();
        addMetricBacklog(input, processes);
        addThroughputMetric(input, processes);
        addProductivityMetric();
        return CurrentStatusData.builder().processes(processes).build();
    }

    private void addMetricBacklog(GetMonitorInput input, ArrayList<Process> processes) {
        final String status = "status";
        final List<Map<String, String>> statuses = List.of(
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

    private void addThroughputMetric(final GetMonitorInput input, 
            final ArrayList<Process> processes) {
        final List<UnitsResume> unitsLastHour = analyticsClient
                .getUnitsInInterval(input.getWarehouseId(), 
                        1, Arrays.asList(AnalyticsQueryEvent.PACKING_FINISH,
                                AnalyticsQueryEvent.PICKUP_FINISH));
        unitsLastHour.forEach(unitLastHour -> addMetricToProcess(unitLastHour, processes));
        
    }

    private void addMetricToProcess(UnitsResume unitLastHour, ArrayList<Process> processes) {
        AnalyticsQueryEvent eventType = unitLastHour.getProcess();
        Process relatedProcess = processes.stream().filter(process 
                -> Objects.equals(process.getTitle(), 
                        eventType.getRelatedProcess()))
                .findFirst()
                .orElse(null);
        
        if (Objects.nonNull(relatedProcess)) {
            Metric metric = Metric.builder()
                    .title(THROUGHPUT_PER_HOUR.getTitle())
                    .type(THROUGHPUT_PER_HOUR.getType())
                    .subtitle(ProcessInfo.getByTitle(eventType
                           .getRelatedProcess())
                           .getSubtitle())
                    .value(unitLastHour.getUnitCount() + " uds./h")                   
                    .build();
            relatedProcess.getMetrics().add(metric);                    
        }
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
            final Metric backlogMetric = Metric.builder()
                    .type(BACKLOG.getType())
                    .title(BACKLOG.getTitle())
                    .subtitle(processInfo.getSubtitle())
                    .value(quantity + " uds.")
                    .build();
            final Process process = Process.builder()
                    .title(processInfo.getTitle())
                    .metrics(new LinkedList<>())
                    .build();
            process.getMetrics().add(backlogMetric);
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
