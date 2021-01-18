package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.GetBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.GetProductivity;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.ProductivityInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.GetThroughput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.ThroughputInput;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_NO_WALL;
import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createEmptyMetric;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@Slf4j
@Named
@AllArgsConstructor
public class GetCurrentStatus implements UseCase<GetMonitorInput, CurrentStatusData> {

    private static final String STATUS_ATTRIBUTE = "status";
    private static final int HOURS_OFFSET = 1;
    private final AnalyticsGateway analyticsClient;
    private final BacklogGatewayProvider backlogGatewayProvider;
    private final GetBacklogMetricUseCase getBacklogMetric;
    private final GetThroughput getThroughputMetric;
    private final GetProductivity getProductivityMetric;
    private final OutboundWaveGateway outboundWaveGateway;

    @Override
    public CurrentStatusData execute(GetMonitorInput input) {
        TreeSet<Process> processes = getProcessesAndMetrics(input);
        return CurrentStatusData.builder().processes(processes).build();
    }

    private TreeSet<Process> getProcessesAndMetrics(GetMonitorInput input) {
        final TreeSet<Process> processes = new TreeSet<>();
        final List<ProcessBacklog> processBacklogs = getProcessBacklogs(input);
        final List<UnitsResume> processedUnitsLastHour = getUnitsResumes(input);
        processBacklogs.forEach(processBacklog ->
                addProcessIfExist(processes, processBacklog, input, processedUnitsLastHour)
        );
        completeProcess(processes);
        return processes;
    }

    private List<ProcessBacklog> getProcessBacklogs(GetMonitorInput input) {
        final String status = STATUS_ATTRIBUTE;
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
        //TODO Aqui se debe colocar el get que obtiene el backlog real de packing wall
        final ProcessBacklog packingWall = ProcessBacklog.builder()
                .process(ProcessInfo.PACKING_WALL.getStatus())
                .quantity(0)
                .build();

        processBacklogs.addAll(Arrays.asList(pickingBacklog, wallInBacklog, packingWall));
        return processBacklogs;
    }

    private List<UnitsResume> getUnitsResumes(final GetMonitorInput input) {
        try {
            return analyticsClient.getUnitsInInterval(input.getWarehouseId(), HOURS_OFFSET,
                    asList(PACKING_WALL, AnalyticsQueryEvent.PICKING, PACKING_NO_WALL)
            );
        } catch (Exception e) {
            log.error(String
                    .format("An error occurred while trying to invoke analytics service: %s",
                            e.getMessage()));
        }
        return emptyList();
    }

    private void addProcessIfExist(final TreeSet<Process> processes,
                                   final ProcessBacklog processBacklog,
                                   final GetMonitorInput input,
                                   final List<UnitsResume> processedUnitsLastHour) {
        final Runnable packingWallIsNotPresent = getRunnableProcessToAdd(
                processes, processBacklog, input, processedUnitsLastHour, () -> { }, WALL_IN
        );
        final Runnable packingIsNotPresent = getRunnableProcessToAdd(processes, processBacklog,
                input, processedUnitsLastHour, packingWallIsNotPresent, ProcessInfo.PACKING_WALL
        );
        final Runnable pickingIsNotPresent = getRunnableProcessToAdd(processes, processBacklog,
                input, processedUnitsLastHour, packingIsNotPresent, PACKING
        );
        final Runnable outboundPlanningIsNotPresent = getRunnableProcessToAdd(processes,
                processBacklog, input, processedUnitsLastHour, pickingIsNotPresent, PICKING
        );
        getProcessBy(processBacklog, OUTBOUND_PLANNING, input, getUnitsCountWaves(input)
        ).ifPresentOrElse(processes::add, outboundPlanningIsNotPresent);
    }

    private Runnable getRunnableProcessToAdd(final TreeSet<Process> processes,
                                             final ProcessBacklog processBacklog,
                                             final GetMonitorInput input,
                                             final List<UnitsResume> processedUnitsLastHour,
                                             final Runnable runnableOrElseMethod,
                                             final ProcessInfo processInfo) {
        return () -> getProcessBy(processBacklog,
                processInfo, input, getUnitResumeForProcess(processInfo, processedUnitsLastHour)
        ).ifPresentOrElse(processes::add, runnableOrElseMethod);
    }

    private void completeProcess(final TreeSet<Process> processes) {
        final List<ProcessInfo> processList = List.of(OUTBOUND_PLANNING, PACKING);
        processList.stream().filter(t -> processes.stream()
                .noneMatch(current -> current.getTitle().equalsIgnoreCase(t.getTitle()))
        ).forEach(noneMatchProcess -> {
                    Process process = Process.builder()
                            .metrics(createEmptyMetricList(noneMatchProcess))
                            .title(noneMatchProcess.getTitle())
                            .build();
                    processes.add(process);
        }
        );
    }

    private Optional<Process> getProcessBy(final ProcessBacklog processBacklog,
                                           final ProcessInfo processInfo,
                                           final GetMonitorInput input,
                                           final UnitsResume unitResume) {
        if (processBacklog.getProcess().equalsIgnoreCase(processInfo.getStatus())) {
            final Process process = Process.builder()
                    .title(processInfo.getTitle())
                    .metrics(createMetricsList(processBacklog, processInfo, input, unitResume))
                    .build();
            return Optional.of(process);
        }
        return Optional.empty();
    }

    private UnitsResume getUnitResumeForProcess(final ProcessInfo processInfo,
                                                final List<UnitsResume> processedUnitsLastHour) {
        if (processedUnitsLastHour.isEmpty()) {
            return null;
        }
        return processedUnitsLastHour.stream().filter(unitsLastHour
                -> Objects.equals(processInfo.getTitle(),
                unitsLastHour.getProcess().getRelatedProcess()))
                .findFirst()
                .orElse(null);
    }

    private UnitsResume getUnitsCountWaves(GetMonitorInput input) {
        return outboundWaveGateway.getUnitsCount(
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo(),
                "ORDER");
    }

    private List<Metric> createMetricsList(final ProcessBacklog processBacklog,
                                           final ProcessInfo processInfo,
                                           final GetMonitorInput input,
                                           final UnitsResume unitResume) {
        List<Metric> metrics = new ArrayList<>();
        processInfo.getMetricTypes().forEach(metricType -> {
            switch (metricType) {
                case BACKLOG:
                    metrics.add(getBacklogMetric.execute(BacklogMetricInput.builder()
                            .quantity(processBacklog.getQuantity())
                            .processInfo(processInfo)
                            .build()
                    ));
                    break;
                case THROUGHPUT_PER_HOUR:
                    metrics.add(
                            getThroughputMetric.execute(ThroughputInput.builder()
                                    .processInfo(processInfo)
                                    .processedUnitLastHour(unitResume)
                                    .build()
                            )
                    );
                    break;
                case PRODUCTIVITY:
                    metrics.add(
                            getProductivityMetric.execute(
                                    ProductivityInput.builder()
                                            .monitorInput(input)
                                            .processedUnitLastHour(unitResume)
                                            .processInfo(processInfo)
                                            .build()
                            )
                    );
                    break;
                default:
                    break;
            }
        });
        return metrics;
    }

    private List<Metric> createEmptyMetricList(ProcessInfo processInfo) {
        final Metric metric = Metric.builder()
                .type(BACKLOG.getType())
                .title(BACKLOG.getTitle())
                .subtitle(processInfo.getSubtitle())
                .value("0 uds.")
                .build();
        Map<ProcessInfo, List<Metric>> emptyMetrics = Map.of(OUTBOUND_PLANNING, List.of(metric,
                createEmptyMetric(THROUGHPUT_PER_HOUR, processInfo)
                ),
                PACKING, List.of(metric,
                        createEmptyMetric(THROUGHPUT_PER_HOUR, processInfo),
                        createEmptyMetric(PRODUCTIVITY, processInfo)));
        return emptyMetrics.get(processInfo);
    }

}