package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.BATCH_SORTED;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createEmptyMetric;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
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
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetCurrentStatus implements UseCase<GetCurrentStatusInput, CurrentStatusData> {


    private static final String PW = "PW";

    private static final int DAYS_FROM = 7;

    private static final int MONTHS_TO = 2;

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
            processBacklogs = getProcessBacklogFromBacklogApi(input, config.isPutToWall());
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
                                                                 final boolean warehouseHasWall) {

        final List<Photo.Group> lastPhotoGroup = backlogGateway.getLastPhoto(new BacklogLastPhotoRequest(
            input.getWarehouseId(),
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            Set.of(Step.PENDING, Step.TO_PICK, Step.TO_ROUTE, Step.TO_PACK, Step.TO_SORT, Step.SORTED, Step.TO_GROUP, Step.GROUPING),
            null,
            null,
            null,
            null,
            Set.of(STEP, AREA, DATE_OUT),
            input.getCurrentTime().toInstant())).getGroups();

        final ProcessBacklog wavingBacklog = getProcessBacklog(lastPhotoGroup, OUTBOUND_PLANNING.getStatus());

        final int immediatePlanningBacklog = getImmediateBacklogFromBacklogApi(input, lastPhotoGroup);

        wavingBacklog.setImmediateQuantity(immediatePlanningBacklog);

        final ProcessBacklog packingBacklog = getProcessBacklog(lastPhotoGroup, PACKING.getStatus());

        final ProcessBacklog pickingBacklog = getProcessBacklog(lastPhotoGroup, PICKING.getStatus());

        final List<ProcessBacklog> processBacklogs = new ArrayList<>(List.of(wavingBacklog, packingBacklog, pickingBacklog));

        if (warehouseHasWall) {
            final ProcessBacklog wallInBacklog = getProcessBacklog(lastPhotoGroup, WALL_IN.getStatus());

            final ProcessBacklog batchSorted = getProcessBacklog(lastPhotoGroup, BATCH_SORTED.getStatus());

            final ProcessBacklog packingWall = ProcessBacklog.builder()
                    .process(PACKING_WALL.getStatus())
                    .quantity(lastPhotoGroup.stream()
                            .filter(item -> PW.equals(item.getKey().get(AREA))
                                && PACKING.getStatus().equalsIgnoreCase(item.getKey().get(STEP)))
                            .mapToInt(Photo.Group::getTotal).sum())
                    .area(PW)
                    .build();

            packingBacklog.setQuantity(packingBacklog.getQuantity() - packingWall.getQuantity());
            processBacklogs.addAll(Arrays.asList(batchSorted, wallInBacklog, packingWall));
        }

        return processBacklogs;
    }

    private ProcessBacklog getProcessBacklog(final List<Photo.Group> groupList, final String processName) {
        return ProcessBacklog.builder()
                .process(processName)
                .quantity(groupList.stream()
                        .filter(item -> {
                            final List<String> steps = Arrays.asList(processName.toUpperCase(Locale.ROOT).split(","));
                            return steps.contains(item.getKey().get(STEP));
                        })
                        .mapToInt(Photo.Group::getTotal).sum())
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

            final ProcessBacklog batchSorted = backlogGateway
                .getUnitBacklog(
                    new UnitProcessBacklogInput(
                        BATCH_SORTED.getStatus(),
                        input.getWarehouseId(),
                        cptFrom,
                        cptTo,
                        null,
                        input.getGroupType()));

            final ProcessBacklog packingWall = backlogGateway
                    .getUnitBacklog(
                            new UnitProcessBacklogInput(ProcessOutbound.PACKING_WALL.getStatus(),
                                    input.getWarehouseId(), cptFrom, cptTo, PW,
                                    input.getGroupType()));

            packingBacklog.setQuantity(packingBacklog.getQuantity() - packingWall.getQuantity());
            processBacklogs.addAll(Arrays.asList(batchSorted, wallInBacklog, packingWall));
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
                                                  final List<Photo.Group> backlogConsolidation) {
        final ZonedDateTime yesterday = input.getCurrentTime()
                .minusDays(1)
                .withZoneSameInstant(UTC);

        final ZonedDateTime tomorrow = input.getCurrentTime()
                .plusDays(1)
                .withZoneSameInstant(UTC);

        return backlogConsolidation.stream()
                .filter(item -> item.getKey().get(STEP).equals(OUTBOUND_PLANNING.getStatus().toUpperCase(Locale.ROOT))
                        && ZonedDateTime.parse(item.getKey().get(DATE_OUT)).isAfter(yesterday)
                        && ZonedDateTime.parse(item.getKey().get(DATE_OUT)).isBefore(tomorrow))
                .mapToInt(Photo.Group::getTotal).sum();
    }

    private void addProcessIfExist(final TreeSet<Process> processes,
                                   final CurrentStatusMetricInputs inputs) {

        final Runnable packingWallIsNotPresent = getRunnableProcessToAdd(
                processes, inputs, () -> {
                }, WALL_IN
        );

        final Runnable batchIsNotPresent = getRunnableProcessToAdd(processes,
            inputs, packingWallIsNotPresent, BATCH_SORTED
        );

        final Runnable packingIsNotPresent = getRunnableProcessToAdd(processes,
                inputs, batchIsNotPresent, PACKING_WALL
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

