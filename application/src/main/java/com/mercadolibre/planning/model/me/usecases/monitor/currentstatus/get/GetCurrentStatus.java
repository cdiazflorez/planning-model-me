package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createEmptyMetric;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.GetBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.immediatebacklog.get.GetImmediateBacklogMetricUseCase;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetCurrentStatus implements UseCase<GetCurrentStatusInput, CurrentStatusData> {

    private static final String AREA = "area";

    private static final String STEP = "step";

    private static final String DATE_OUT = "date_out";

    private static final String PW = "PW";

    private static final int DAYS_FROM = 7;

    private static final int MONTHS_TO = 2;

    private static final List<String> GROUPERS = List.of(STEP, AREA, DATE_OUT);

    private static final List<String> PROCESSES =
            List.of(OUTBOUND_PLANNING.getStatus(), PACKING.getStatus(), PICKING.getStatus(), WALL_IN.getStatus(), PACKING_WALL.getStatus());

    private final GetBacklogMetricUseCase getBacklogMetric;

    private final GetImmediateBacklogMetricUseCase getImmediateBacklogMetric;

    private final LogisticCenterGateway logisticCenterGateway;

    private final BacklogGatewayProvider backlogGatewayProvider;

    private final FeatureSwitches featureSwitches;

    private final BacklogApiGateway backlogGateway;

    @Override
    public CurrentStatusData execute(final GetCurrentStatusInput input) {
        final TreeSet<Process> processes = getProcessesAndMetrics(input);
        return CurrentStatusData.builder().processes(processes).build();
    }

    private TreeSet<Process> getProcessesAndMetrics(final GetCurrentStatusInput input) {
        final TreeSet<Process> processes = new TreeSet<>();
        final List<ProcessBacklog> processBacklogs;

        final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(input.getWarehouseId());
        final ZonedDateTime cptFrom = input.getCurrentTime().truncatedTo(DAYS).minusDays(DAYS_FROM).withZoneSameInstant(UTC);
        final ZonedDateTime cptTo = input.getCurrentTime().truncatedTo(DAYS).plusMonths(MONTHS_TO).withZoneSameInstant(UTC);

        if (featureSwitches.shouldCallBacklogApi()) {
            processBacklogs = getProcessBacklogFromBacklogApi(input, cptFrom.toInstant(), cptTo.toInstant(), config.isPutToWall());
        } else {
            processBacklogs = getProcessBacklogsFromOutboundUnit(input, cptFrom, cptTo, config.isPutToWall());
        }

        completeBacklogs(processBacklogs);

        processBacklogs.forEach(processBacklog ->
                addProcessIfExist(processes, CurrentStatusMetricInputs.builder()
                        .input(input)
                        .processBacklog(processBacklog)
                        .processedUnitsLastHour(emptyList())
                        .build())
        );

        completeProcess(processes);
        return processes;
    }

    private List<ProcessBacklog> getProcessBacklogFromBacklogApi(final GetCurrentStatusInput input,
                                                                 final Instant cptDateFrom,
                                                                 final Instant cptDateTo,
                                                                 final boolean warehouseHasWall) {

        final List<Consolidation> backlogConsolidation = backlogGateway.getCurrentBacklog(new BacklogCurrentRequest(
                input.getCurrentTime().toInstant(),
                input.getWarehouseId(),
                List.of("outbound-orders"),
                emptyList(),
                PROCESSES,
                GROUPERS,
                cptDateFrom,
                cptDateTo));

        final ProcessBacklog wavingBacklog = getProcessBacklog(backlogConsolidation, OUTBOUND_PLANNING.getStatus());

        final int immediatePlanningBacklog = getImmediateBacklogFromBacklogApi(input, backlogConsolidation);

        wavingBacklog.setImmediateQuantity(immediatePlanningBacklog);

        final ProcessBacklog packingBacklog = getProcessBacklog(backlogConsolidation, PACKING.getStatus());

        final ProcessBacklog pickingBacklog = getProcessBacklog(backlogConsolidation, PICKING.getStatus());

        final List<ProcessBacklog> processBacklogs = new ArrayList<>(List.of(wavingBacklog, packingBacklog, pickingBacklog));

        if (warehouseHasWall) {
            final ProcessBacklog wallInBacklog = ProcessBacklog.builder()
                    .process(WALL_IN.getStatus())
                    .quantity(backlogConsolidation.stream()
                            .filter(item -> {
                                final List<String> steps = Arrays.asList(WALL_IN.getStatus().toUpperCase(Locale.ROOT).split(","));
                                return steps.contains(item.getKeys().get(STEP));
                            })
                            .mapToInt(Consolidation::getTotal).sum())
                    .build();

            final ProcessBacklog packingWall = ProcessBacklog.builder()
                    .process(PACKING_WALL.getStatus())
                    .quantity(backlogConsolidation.stream()
                            .filter(item -> PW.equals(item.getKeys().get(AREA)))
                            .mapToInt(Consolidation::getTotal).sum())
                    .area(PW)
                    .build();

            packingBacklog.setQuantity(packingBacklog.getQuantity() - packingWall.getQuantity());
            processBacklogs.addAll(Arrays.asList(wallInBacklog, packingWall));
        }

        return processBacklogs;
    }

    private ProcessBacklog getProcessBacklog(final List<Consolidation> consolidations, final String processName) {
        return ProcessBacklog.builder()
                .process(processName)
                .quantity(consolidations.stream()
                        .filter(item -> item.getKeys().get(STEP).equals(processName.toUpperCase(Locale.ROOT)))
                        .mapToInt(Consolidation::getTotal).sum())
                .build();
    }

    private List<ProcessBacklog> getProcessBacklogsFromOutboundUnit(final GetCurrentStatusInput input,
                                                                    final ZonedDateTime cptFrom,
                                                                    final ZonedDateTime cptTo,
                                                                    final boolean warehouseHasWall) {

        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()));

        final List<ProcessBacklog> processBacklogs = new ArrayList<>();

        final ProcessBacklog wavingBacklog = backlogGateway.getUnitBacklog(
                new UnitProcessBacklogInput(OUTBOUND_PLANNING.getStatus(), input.getWarehouseId(),
                        cptFrom, cptTo, null, input.getGroupType()));

        final int immediatePlanningBacklog = getImmediateBacklogFromOutboundUnit(input, backlogGateway);

        wavingBacklog.setImmediateQuantity(immediatePlanningBacklog);

        final ProcessBacklog packingBacklog = backlogGateway.getUnitBacklog(
                new UnitProcessBacklogInput(PACKING.getStatus(), input.getWarehouseId(),
                        cptFrom, cptTo, null, input.getGroupType()));

        processBacklogs.add(wavingBacklog);
        processBacklogs.add(packingBacklog);

        final ProcessBacklog pickingBacklog = backlogGateway.getUnitBacklog(
                new UnitProcessBacklogInput(PICKING.getStatus(), input.getWarehouseId(),
                        cptFrom, cptTo, null, input.getGroupType()));

        processBacklogs.add(pickingBacklog);

        if (warehouseHasWall) {
            final ProcessBacklog wallInBacklog = backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, null, input.getGroupType()));

            final ProcessBacklog packingWall = backlogGateway
                    .getUnitBacklog(
                            new UnitProcessBacklogInput(ProcessOutbound.PACKING_WALL.getStatus(),
                                    input.getWarehouseId(), cptFrom, cptTo, PW,
                                    input.getGroupType()));

            packingBacklog.setQuantity(packingBacklog.getQuantity() - packingWall.getQuantity());
            processBacklogs.addAll(Arrays.asList(wallInBacklog, packingWall));
        }

        return processBacklogs;
    }

    private int getImmediateBacklogFromOutboundUnit(final GetCurrentStatusInput input,
                                                    final BacklogGateway backlogGateway) {
        final ZonedDateTime yesterday = input.getCurrentTime()
                .minusDays(1)
                .withZoneSameInstant(UTC);

        final ZonedDateTime tomorrow = input.getCurrentTime()
                .plusDays(1)
                .withZoneSameInstant(UTC);

        return backlogGateway.getUnitBacklog(
                        new UnitProcessBacklogInput(
                                OUTBOUND_PLANNING.getStatus(),
                                input.getWarehouseId(),
                                yesterday,
                                tomorrow,
                                null,
                                input.getGroupType()))
                .getQuantity();
    }

    private int getImmediateBacklogFromBacklogApi(final GetCurrentStatusInput input,
                                                  final List<Consolidation> backlogConsolidation) {
        final ZonedDateTime yesterday = input.getCurrentTime()
                .minusDays(1)
                .withZoneSameInstant(UTC);

        final ZonedDateTime tomorrow = input.getCurrentTime()
                .plusDays(1)
                .withZoneSameInstant(UTC);

        return backlogConsolidation.stream()
                .filter(item -> item.getKeys().get(STEP).equals(OUTBOUND_PLANNING.getStatus().toUpperCase(Locale.ROOT))
                        && ZonedDateTime.parse(item.getKeys().get(DATE_OUT)).isAfter(yesterday)
                        && ZonedDateTime.parse(item.getKeys().get(DATE_OUT)).isBefore(tomorrow))
                .mapToInt(Consolidation::getTotal).sum();
    }

    private void addProcessIfExist(final TreeSet<Process> processes,
                                   final CurrentStatusMetricInputs inputs) {

        final Runnable packingWallIsNotPresent = getRunnableProcessToAdd(
                processes, inputs, () -> {
                }, WALL_IN
        );
        final Runnable packingIsNotPresent = getRunnableProcessToAdd(processes,
                inputs, packingWallIsNotPresent, ProcessOutbound.PACKING_WALL
        );
        final Runnable pickingIsNotPresent = getRunnableProcessToAdd(processes,
                inputs, packingIsNotPresent, PACKING
        );
        final Runnable outboundPlanningIsNotPresent = getRunnableProcessToAdd(processes,
                inputs, pickingIsNotPresent, PICKING
        );

        getProcessBy(inputs, OUTBOUND_PLANNING).ifPresentOrElse(processes::add, outboundPlanningIsNotPresent);
    }

    private Runnable getRunnableProcessToAdd(final TreeSet<Process> processes,
                                             final CurrentStatusMetricInputs inputs,
                                             final Runnable runnableOrElseMethod,
                                             final ProcessOutbound processOutbound) {
        return () -> getProcessBy(inputs, processOutbound).ifPresentOrElse(processes::add, runnableOrElseMethod);
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
                                           final ProcessOutbound processOutbound) {

        if (inputs.getProcessBacklog().getProcess().equalsIgnoreCase(processOutbound.getStatus())
                && Objects.equals(inputs.getProcessBacklog().getArea(),
                getProcessInfoArea(processOutbound))) {

            final Process process = Process.builder()
                    .title(processOutbound.getTitle())
                    .metrics(createMetricsList(inputs, processOutbound))
                    .build();
            return Optional.of(process);
        }
        return Optional.empty();
    }


    private String getProcessInfoArea(ProcessOutbound processOutbound) {
        Map<ProcessOutbound, String> processAreas = Map.of(PACKING_WALL, PW);
        return processAreas.get(processOutbound);
    }

    private List<Metric> createMetricsList(final CurrentStatusMetricInputs inputs,
                                           final ProcessOutbound processOutbound) {
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
                default:
                    break;
            }
        });
        return metrics;
    }

    private List<Metric> createEmptyMetricList(final ProcessOutbound processOutbound) {
        final Metric metric = Metric.builder()
                .type(TOTAL_BACKLOG.getType())
                .title(TOTAL_BACKLOG.getTitle())
                .subtitle(processOutbound.getSubtitle())
                .value("10 uds.")
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

