package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
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
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;

@Slf4j
@Named
@AllArgsConstructor
public class GetCurrentStatus implements UseCase<GetMonitorInput, CurrentStatusData> {

    private static final List<ProcessingType> PROJECTION_PROCESSING_TYPES =
            List.of(ProcessingType.ACTIVE_WORKERS);

    private static final String STATUS_ATTRIBUTE = "status";
    private static final int HOURS_OFFSET = 1;
    private final AnalyticsGateway analyticsClient;
    private final BacklogGatewayProvider backlogGatewayProvider;
    private final GetBacklogMetricUseCase getBacklogMetric;
    private final GetThroughput getThroughputMetric;
    private final GetProductivity getProductivityMetric;
    private final OutboundWaveGateway outboundWaveGateway;
    private final PlanningModelGateway planningModelGateway;
    private final LogisticCenterGateway logisticCenterGateway;

    @Override
    public CurrentStatusData execute(GetMonitorInput input) {
        TreeSet<Process> processes = getProcessesAndMetrics(input);
        return CurrentStatusData.builder().processes(processes).build();
    }

    private TreeSet<Process> getProcessesAndMetrics(GetMonitorInput input) {
        final TreeSet<Process> processes = new TreeSet<>();
        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());
        final List<ProcessBacklog> processBacklogs = getProcessBacklogs(input,
                config.isPutToWall());
        completeBacklogs(processBacklogs);
        final List<UnitsResume> processedUnitsLastHour = getUnitsResumes(input,
                config.isPutToWall());
        final List<Entity> productivityHeadCounts =
                getHeadcountForProductivity(input, processedUnitsLastHour);

        processBacklogs.forEach(processBacklog ->
                addProcessIfExist(processes, CurrentStatusMetricInputs.builder()
                        .input(input)
                        .processBacklog(processBacklog)
                        .processedUnitsLastHour(processedUnitsLastHour)
                        .productivityHeadCounts(productivityHeadCounts)
                        .build())
        );

        completeProcess(processes);
        return processes;
    }

    private List<Entity> getHeadcountForProductivity(final GetMonitorInput input,
                         final List<UnitsResume> processedUnitsLastHour) {
        final ZonedDateTime utcDateTo = DateUtils.getCurrentUtcDateTime()
                .with(ChronoField.MINUTE_OF_HOUR, 0);
        final ZonedDateTime utcDateFrom = utcDateTo.minusHours(1);
        return planningModelGateway.getEntities(
                createHeadcountRequest(input,
                        utcDateFrom,
                        utcDateTo,
                        processedUnitsLastHour
                ));
    }

    private EntityRequest createHeadcountRequest(final GetMonitorInput input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<UnitsResume> unitsResumes) {
        return EntityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(EntityType.HEADCOUNT)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(unitsResumes.stream().map(unitResume
                        -> ProcessName.from(unitResume.getProcess().getRelatedProcessName()))
                        .collect(Collectors.toList()))
                .processingType(PROJECTION_PROCESSING_TYPES)
                .build();
    }

    private List<ProcessBacklog> getProcessBacklogs(final GetMonitorInput input,
                                                    final boolean warehouseHasWall) {
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
        final ProcessBacklog pickingBacklog =
                backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(PICKING.getStatus(),
                        input.getWarehouseId(),
                        input.getDateFrom(),
                        input.getDateTo(), null));
        if (warehouseHasWall) {
            final ProcessBacklog wallInBacklog = backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(),
                            input.getWarehouseId(),
                            input.getDateFrom(),
                            input.getDateTo(), null));
            final ProcessBacklog packingWall = backlogGateway
                    .getUnitBacklog(new UnitProcessBacklogInput(
                            ProcessInfo.PACKING_WALL.getStatus(),
                            input.getWarehouseId(),
                            input.getDateFrom(),
                            input.getDateTo(), "PW"));
            recalculatePackingNoWallUnits(processBacklogs, packingWall);
            processBacklogs.addAll(Arrays.asList(pickingBacklog, wallInBacklog, packingWall));
        } else {
            recalculatePackingNoWallUnits(processBacklogs, null);
            processBacklogs.add(pickingBacklog);
        }
        return processBacklogs;
    }

    private void recalculatePackingNoWallUnits(final List<ProcessBacklog> processBacklogs,
                                               final ProcessBacklog packingWall) {
        processBacklogs.stream().filter(backlog
                -> backlog.getProcess().equals(PACKING.getTitle()))
                .forEach(backlog -> {
                    if (nonNull(packingWall)) {
                        backlog.setQuantity(
                                backlog.getQuantity() - packingWall.getQuantity()
                        );
                    } else {
                        backlog.setQuantity(backlog.getQuantity());
                    }
                });
    }

    private List<UnitsResume> getUnitsResumes(final GetMonitorInput input,
                                              final boolean havePutToWall) {
        try {
            final List<AnalyticsQueryEvent> queryEvents = havePutToWall
                    ? asList(PACKING_WALL, AnalyticsQueryEvent.PICKING, PACKING_NO_WALL) :
                    singletonList(AnalyticsQueryEvent.PICKING)
                    ;
            return analyticsClient.getUnitsInInterval(input.getWarehouseId(), HOURS_OFFSET,
                    queryEvents);
        } catch (Exception e) {
            log.error(String
                    .format("An error occurred while trying to invoke analytics service: %s",
                            e.getMessage()));
        }
        return emptyList();
    }

    private void addProcessIfExist(final TreeSet<Process> processes,
                                   CurrentStatusMetricInputs inputs) {

        final Runnable packingWallIsNotPresent = getRunnableProcessToAdd(
                processes, inputs, () -> { }, WALL_IN
        );
        final Runnable packingIsNotPresent = getRunnableProcessToAdd(processes,
                inputs, packingWallIsNotPresent, ProcessInfo.PACKING_WALL
        );
        final Runnable pickingIsNotPresent = getRunnableProcessToAdd(processes,
                inputs, packingIsNotPresent, PACKING
        );
        final Runnable outboundPlanningIsNotPresent  = getRunnableProcessToAdd(processes,
                inputs, pickingIsNotPresent, PICKING
        );

        getProcessBy(inputs, OUTBOUND_PLANNING,
                getUnitsCountWaves(inputs.getInput())
        ).ifPresentOrElse(processes::add, outboundPlanningIsNotPresent);
    }

    private Runnable getRunnableProcessToAdd(final TreeSet<Process> processes,
                                             final CurrentStatusMetricInputs inputs,
                                             final Runnable runnableOrElseMethod,
                                             final ProcessInfo processInfo) {
        return () -> getProcessBy(inputs,
                processInfo,getUnitResumeForProcess(processInfo,
                        inputs.getProcessedUnitsLastHour())
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

    private Optional<Process> getProcessBy(final CurrentStatusMetricInputs inputs,
                                           final ProcessInfo processInfo,
                                           final UnitsResume unitResume) {

        if (inputs.getProcessBacklog().getProcess().equalsIgnoreCase(processInfo.getStatus())
                && Objects.equals(inputs.getProcessBacklog().getArea(),
                getProcessInfoArea(processInfo))) {

            final Process process = Process.builder()
                    .title(processInfo.getTitle())
                    .metrics(createMetricsList(inputs, processInfo, unitResume))
                    .build();
            return Optional.of(process);
        }
        return Optional.empty();
    }


    private String getProcessInfoArea(ProcessInfo processInfo) {
        Map<ProcessInfo,String> processAreas = Map.of(ProcessInfo.PACKING_WALL, "PW");
        return processAreas.get(processInfo);
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
        ZonedDateTime currentTime = getCurrentUtcDateTime();
        return outboundWaveGateway.getUnitsCount(
                input.getWarehouseId(),
                currentTime.minusHours(1),
                currentTime,
                "ORDER");
    }

    private List<Metric> createMetricsList(final CurrentStatusMetricInputs inputs,
                                           final ProcessInfo processInfo,
                                           final UnitsResume unitResume) {
        List<Metric> metrics = new ArrayList<>();
        processInfo.getMetricTypes().forEach(metricType -> {
            switch (metricType) {
                case BACKLOG:
                    metrics.add(getBacklogMetric.execute(BacklogMetricInput.builder()
                            .quantity(inputs.getProcessBacklog().getQuantity())
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
                                            .monitorInput(inputs.getInput())
                                            .processedUnitLastHour(unitResume)
                                            .processInfo(processInfo)
                                            .headcounts(inputs.getProductivityHeadCounts())
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

    private void completeBacklogs(List<ProcessBacklog> processBacklogs) {
        final List<ProcessInfo> processList = List.of(OUTBOUND_PLANNING, PACKING);

        processList.stream().filter(t -> processBacklogs.stream()
                .noneMatch(current -> current.getProcess().equalsIgnoreCase(t.getStatus())
                        && current.getArea() == null)
        ).forEach(noneMatchProcess -> {
                    ProcessBacklog process = ProcessBacklog.builder()
                            .process(noneMatchProcess.getStatus())
                            .quantity(0)
                            .build();
                    processBacklogs.add(process);
        }
        );
    }
}

