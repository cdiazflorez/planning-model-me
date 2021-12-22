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
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.GetBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.immediatebacklog.get.GetImmediateBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.GetThroughput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.ThroughputInput;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_NO_WALL;
import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createEmptyMetric;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;

@Slf4j
@Named
@AllArgsConstructor
public class GetCurrentStatus implements UseCase<GetCurrentStatusInput, CurrentStatusData> {

    private static final String STATUS_ATTRIBUTE = "status";
    private static final int HOURS_OFFSET = 1;
    private final AnalyticsGateway analyticsClient;
    private final BacklogGatewayProvider backlogGatewayProvider;
    private final GetBacklogMetricUseCase getBacklogMetric;
    private final GetImmediateBacklogMetricUseCase getImmediateBacklogMetric;
    private final GetThroughput getThroughputMetric;
    private final OutboundWaveGateway outboundWaveGateway;
    private final LogisticCenterGateway logisticCenterGateway;

    @Override
    public CurrentStatusData execute(GetCurrentStatusInput input) {
        TreeSet<Process> processes = getProcessesAndMetrics(input);
        return CurrentStatusData.builder().processes(processes).build();
    }

    private TreeSet<Process> getProcessesAndMetrics(GetCurrentStatusInput input) {
        final TreeSet<Process> processes = new TreeSet<>();

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                input.getWarehouseId());

        final List<ProcessBacklog> processBacklogs = getProcessBacklogs(input,
                config.isPutToWall());

        completeBacklogs(processBacklogs);

        final List<UnitsResume> processedUnitsLastHour = getUnitsResumes(input,
                config.isPutToWall());

        processBacklogs.forEach(processBacklog ->
                addProcessIfExist(processes, CurrentStatusMetricInputs.builder()
                        .input(input)
                        .processBacklog(processBacklog)
                        .processedUnitsLastHour(processedUnitsLastHour)
                        .build())
        );

        completeProcess(processes);
        return processes;
    }

    private List<ProcessBacklog> getProcessBacklogs(final GetCurrentStatusInput input,
                                                    final boolean warehouseHasWall) {

        final ZonedDateTime cptFrom = input.getCurrentTime().truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = input.getCurrentTime().truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        final List<Map<String, String>> statuses = List.of(
                Map.of(STATUS_ATTRIBUTE, OUTBOUND_PLANNING.getStatus()),
                Map.of(STATUS_ATTRIBUTE, PACKING.getStatus())
        );

        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()));

        final List<ProcessBacklog> processBacklogs = backlogGateway.getBacklog(statuses,
                input.getWarehouseId(), cptFrom, cptTo, false);

        final ProcessBacklog pickingBacklog = backlogGateway.getUnitBacklog(
                new UnitProcessBacklogInput(PICKING.getStatus(), input.getWarehouseId(),
                        cptFrom, cptTo, null, input.getGroupType(), false));

        if (warehouseHasWall) {
            final ProcessBacklog wallInBacklog = backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, null, input.getGroupType(), false));

            final ProcessBacklog packingWall = backlogGateway
                    .getUnitBacklog(
                            new UnitProcessBacklogInput(ProcessOutbound.PACKING_WALL.getStatus(),
                            input.getWarehouseId(), cptFrom, cptTo, "PW",
                                    input.getGroupType(), false));

            recalculatePackingNoWallUnits(processBacklogs, packingWall);
            processBacklogs.addAll(Arrays.asList(pickingBacklog, wallInBacklog, packingWall));
        } else {
            recalculatePackingNoWallUnits(processBacklogs, null);
            processBacklogs.add(pickingBacklog);
        }

        final ProcessBacklog immediatePlanningBacklog = getImmediateBacklog(input, backlogGateway);

        processBacklogs.stream()
                .filter(p -> OUTBOUND_PLANNING.getStatus().equals(p.getProcess()))
                .forEach(p -> p.setImmediateQuantity(immediatePlanningBacklog.getQuantity()));

        return processBacklogs;
    }

    private ProcessBacklog getImmediateBacklog(final GetCurrentStatusInput input,
                                               final BacklogGateway backlogGateway) {
        final ZonedDateTime yesterday = input.getCurrentTime()
                .minusDays(1)
                .withZoneSameInstant(UTC);

        final ZonedDateTime tomorrow = input.getCurrentTime()
                .plusDays(1)
                .withZoneSameInstant(UTC);

        return backlogGateway.getUnitBacklog(
                new UnitProcessBacklogInput(OUTBOUND_PLANNING.getStatus(), input.getWarehouseId(),
                        yesterday, tomorrow, null, input.getGroupType(), false));

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

    private List<UnitsResume> getUnitsResumes(final GetCurrentStatusInput input,
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
                inputs, packingWallIsNotPresent, ProcessOutbound.PACKING_WALL
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
                                             final ProcessOutbound processOutbound) {
        return () -> getProcessBy(inputs,
                processOutbound,getUnitResumeForProcess(processOutbound,
                        inputs.getProcessedUnitsLastHour())
        ).ifPresentOrElse(processes::add, runnableOrElseMethod);
    }

    private void completeProcess(final TreeSet<Process> processes) {
        final List<ProcessOutbound> processList = List.of(OUTBOUND_PLANNING, PACKING);

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
                                           final ProcessOutbound processOutbound,
                                           final UnitsResume unitResume) {

        if (inputs.getProcessBacklog().getProcess().equalsIgnoreCase(processOutbound.getStatus())
                && Objects.equals(inputs.getProcessBacklog().getArea(),
                getProcessInfoArea(processOutbound))) {

            final Process process = Process.builder()
                    .title(processOutbound.getTitle())
                    .metrics(createMetricsList(inputs, processOutbound, unitResume))
                    .build();
            return Optional.of(process);
        }
        return Optional.empty();
    }


    private String getProcessInfoArea(ProcessOutbound processOutbound) {
        Map<ProcessOutbound,String> processAreas = Map.of(ProcessOutbound.PACKING_WALL, "PW");
        return processAreas.get(processOutbound);
    }

    private UnitsResume getUnitResumeForProcess(final ProcessOutbound processOutbound,
                                                final List<UnitsResume> processedUnitsLastHour) {
        if (processedUnitsLastHour.isEmpty()) {
            return null;
        }
        return processedUnitsLastHour.stream().filter(unitsLastHour
                -> Objects.equals(processOutbound.getTitle(),
                unitsLastHour.getProcess().getRelatedProcess()))
                .findFirst()
                .orElse(null);
    }

    private UnitsResume getUnitsCountWaves(GetCurrentStatusInput input) {
        ZonedDateTime currentTime = getCurrentUtcDateTime();
        return outboundWaveGateway.getUnitsCount(
                input.getWarehouseId(),
                currentTime.minusHours(1),
                currentTime,
                "order");
    }

    private List<Metric> createMetricsList(final CurrentStatusMetricInputs inputs,
                                           final ProcessOutbound processOutbound,
                                           final UnitsResume unitResume) {
        List<Metric> metrics = new ArrayList<>();
        processOutbound.getMetricTypes().forEach(metricType -> {
            switch (metricType) {
                case TOTAL_BACKLOG:
                    metrics.add(getBacklogMetric.execute(BacklogMetricInput.builder()
                            .quantity(inputs.getProcessBacklog().getQuantity())
                            .processOutbound(processOutbound)
                            .build()
                    ));
                    break;
                case IMMEDIATE_BACKLOG:
                    metrics.add(getImmediateBacklogMetric.execute(BacklogMetricInput.builder()
                            .quantity(inputs.getProcessBacklog().getImmediateQuantity())
                            .processOutbound(processOutbound)
                            .build()
                    ));
                    break;
                case THROUGHPUT_PER_HOUR:
                    metrics.add(
                            getThroughputMetric.execute(ThroughputInput.builder()
                                    .processOutbound(processOutbound)
                                    .processedUnitLastHour(unitResume)
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

    private List<Metric> createEmptyMetricList(ProcessOutbound processOutbound) {
        final Metric metric = Metric.builder()
                .type(TOTAL_BACKLOG.getType())
                .title(TOTAL_BACKLOG.getTitle())
                .subtitle(processOutbound.getSubtitle())
                .value("0 uds.")
                .build();
        Map<ProcessOutbound, List<Metric>> emptyMetrics = Map.of(OUTBOUND_PLANNING, List.of(metric,
                createEmptyMetric(THROUGHPUT_PER_HOUR, processOutbound)
                ),
                PACKING, List.of(metric,
                        createEmptyMetric(THROUGHPUT_PER_HOUR, processOutbound),
                        createEmptyMetric(PRODUCTIVITY, processOutbound)));
        return emptyMetrics.get(processOutbound);
    }

    private void completeBacklogs(List<ProcessBacklog> processBacklogs) {
        final List<ProcessOutbound> processList = List.of(OUTBOUND_PLANNING, PACKING);

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

