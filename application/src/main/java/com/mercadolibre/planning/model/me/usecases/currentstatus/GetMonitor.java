package com.mercadolibre.planning.model.me.usecases.currentstatus;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
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
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_FINISH;
import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PICKUP_FINISH;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.util.Arrays.asList;

@Slf4j
@Named
@AllArgsConstructor
public class GetMonitor implements UseCase<GetMonitorInput, Monitor> {

    private static final int HOURS_OFFSET = 1;
    private static final String STATUS_ATTRIBUTE = "status";
    private static final String UNITS_DEFAULT_STRING = "%d uds.";
    private static final int SELLING_PERIOD_HOURS = 28;
    private static final List<ProcessingType> PROJECTION_PROCESSING_TYPES =
            List.of(ProcessingType.ACTIVE_WORKERS);
    protected static final List<ProcessName> PROJECTION_PROCESS_NAMES =
            List.of(ProcessName.PICKING, ProcessName.PACKING);
    private final BacklogGatewayProvider backlogGatewayProvider;
    protected final LogisticCenterGateway logisticCenterGateway;
    protected final AnalyticsGateway analyticsClient;
    private final GetSales getSales;
    private final PlanningModelGateway planningModelGateway;
    private final OutboundWaveGateway outboundWaveGateway;

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
        final long totalPlanned = getPlannedBacklog(input).stream()
                .mapToLong(PlanningDistributionResponse::getTotal).sum();
        final int totalSales = getSales(input).stream().mapToInt(Backlog::getQuantity).sum();
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
        return totalSold != 0 && totalPlanned != 0
                ? (((double) totalSold / totalPlanned) - 1) * 100
                        : 0;
    }

    private String getIconForDeviation(double totalDeviation) {
        return totalDeviation > 0 ? "arrow_up" : "arrow_down";
    }

    private String getStatusForDeviation(double totalDeviation) {
        return totalDeviation > 0 ? "warning" : null;
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
        TreeSet<Process> processes = getProcessesAndMetrics(input);
        return CurrentStatusData.builder().processes(processes).build();
    }

    private TreeSet<Process> getProcessesAndMetrics(GetMonitorInput input) {
        final String status = STATUS_ATTRIBUTE;
        final TreeSet<Process> processes = new TreeSet<>();
        List<Map<String, String>> statuses = List.of(
                Map.of(status, PACKING.getStatus())
        );
        final BacklogGateway backlogGateway = backlogGatewayProvider.getBy(input.getWorkflow())
                .orElseThrow(() -> new BacklogGatewayNotSupportedException(input.getWorkflow()));
        List<ProcessBacklog> processBacklogs = backlogGateway.getBacklog(statuses,
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo());

        final ProcessBacklog planningBacklog = backlogGateway.getUnitBacklog(
                OUTBOUND_PLANNING.getStatus(),
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
        processBacklogs.addAll(asList(planningBacklog, pickingBacklog, wallInBacklog));
        final List<UnitsResume> processedUnitsLastHour = getUnitsResumes(input);
        processBacklogs.forEach(processBacklog ->
                addProcessIfExist(processes, processBacklog, input, processedUnitsLastHour)
        );
        completeProcess(processes);
        return processes;
    }

    private Metric getThroughputMetric(final ProcessInfo processInfo,
                                       final UnitsResume processedUnitLastHour) {
        if (processedUnitLastHour == null) {
            return createEmptyMetric(THROUGHPUT_PER_HOUR, processInfo);
        }
        return createMetric(processInfo,
                processedUnitLastHour.getUnitCount() + " uds./h",
                THROUGHPUT_PER_HOUR);
    }

    private UnitsResume getUnitsCountWaves(GetMonitorInput input) {
        return outboundWaveGateway.getUnitsCount(
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo(),
                "ORDER");
    }

    private Metric createEmptyMetric(final MetricType metricType, final ProcessInfo process) {
        return createMetric(process, "-", metricType);
    }

    private List<UnitsResume> getUnitsResumes(final GetMonitorInput input) {
        try {
            return analyticsClient
                    .getUnitsInInterval(input.getWarehouseId(),
                            HOURS_OFFSET, asList(PACKING_FINISH, PICKUP_FINISH)
            );
        } catch (Exception e) {
            log.error(String
                    .format("An error occurred while trying to invoke analytics service: %s",
                            e.getMessage()));
        }
        return null;
    }

    private Metric createMetric(ProcessInfo process,
                                    final String value,
                                    MetricType metricType) {
        return Metric.builder()
                    .title(metricType.getTitle())
                    .type(metricType.getType())
                    .subtitle(metricType == MetricType.BACKLOG
                            ? process.getSubtitle() : metricType.getSubtitle())
                    .value(value)
                    .build();

    }

    private Metric getProductivityMetric(final GetMonitorInput input,
                                       final UnitsResume processedUnitLastHour,
                                       final ProcessInfo processInfo) {
        if (processedUnitLastHour == null) {
            return createEmptyMetric(PRODUCTIVITY, processInfo);
        }
        final ZonedDateTime current = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);
        final ZonedDateTime utcDateTo = current.with(ChronoField.MINUTE_OF_HOUR, 0);
        final ZonedDateTime utcDateFrom = utcDateTo.minusHours(1);

        final List<Entity> headcount = planningModelGateway.getEntities(
                createRequest(input, utcDateFrom, utcDateTo
                ));

        return calculateMetric(current, List.of(utcDateTo,
                current.minusHours(1).withSecond(0).withNano(0)), headcount,
                processedUnitLastHour, processInfo);
    }

    private Metric calculateMetric(final ZonedDateTime current,
                                final List<ZonedDateTime> dates,
                                final List<Entity> headcount,
                                final UnitsResume unit,
                                ProcessInfo processInfo) {
        final List<Double> productivities = getProductivities(current, dates, headcount, unit);

        final double productivity = calculateProductivity(productivities);

        return createMetric(processInfo,
                String.format("%.1f %s", productivity, "uds./h"),
                PRODUCTIVITY);
    }

    private List<Double> getProductivities(final ZonedDateTime current,
                                            final List<ZonedDateTime> dates,
                                            final List<Entity> headcount,
                                            final UnitsResume unit) {
        ZonedDateTime toDate = current;
        final List<Double> productivities = new LinkedList<>();
        for (ZonedDateTime utcDate : dates) {
            long timeFromCurrentHour = utcDate.until(toDate, ChronoUnit.MINUTES);
            productivities.add(calculateProductivityForHour(utcDate.withMinute(0), headcount, unit,
                    timeFromCurrentHour));
            toDate = utcDate;
        }
        return productivities;
    }

    private double calculateProductivity(final List<Double> productivities) {
        double productivitySum = productivities.stream().reduce(0d, Double::sum);
        double productivityCount = productivities.stream().filter(prod -> prod > 0).count();
        return productivitySum != 0 && productivityCount != 0
                ? productivitySum / productivityCount
                        : 0;
    }

    private double calculateProductivityForHour(final ZonedDateTime utcDateTo,
                                                final List<Entity> headcount,
                                                UnitsResume unit, long timeFromCurrentHour) {
        final int unitsLastHour = unit.getUnitCount();
        final double percentageCurrentHour = (100.00 * timeFromCurrentHour) / 60;
        final double quantityFromPercentageCurrentHour =
                (unitsLastHour / 100.00) * percentageCurrentHour;
        final int headCountCurrentHour = getProcessHeadcount(utcDateTo, headcount, unit);
        return headCountCurrentHour == 0
                ? 0 : quantityFromPercentageCurrentHour / headCountCurrentHour;
    }

    private int getProcessHeadcount(final ZonedDateTime utcDateFrom, final List<Entity> headcount,
                                    final UnitsResume unit) {
        final Entity headcountForHour = headcount.stream()
                .filter(hCount -> Objects.equals(utcDateFrom, hCount.getDate())
                        && Objects.equals(unit.getProcess().getRelatedProcess().toLowerCase(),
                                hCount.getProcessName().getName())).findFirst().orElse(null);
        if (Objects.nonNull(headcountForHour)) {
            return headcountForHour.getValue();
        }
        return 0;
    }

    private EntityRequest createRequest(final GetMonitorInput input,
                                        final ZonedDateTime dateFrom,
                                        final ZonedDateTime dateTo) {
        return EntityRequest.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(EntityType.HEADCOUNT)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(PROJECTION_PROCESS_NAMES)
                .processingType(GetMonitor.PROJECTION_PROCESSING_TYPES)
                .build();
    }

    private void addProcessIfExist(final TreeSet<Process> processes,
                                   final ProcessBacklog processBacklog,
                                   final GetMonitorInput input,
                                   final List<UnitsResume> processedUnitsLastHour) {
        getProcessBy(processBacklog, OUTBOUND_PLANNING, input,
                getUnitsCountWaves(input))
                .ifPresentOrElse(processes::add,
                        () -> getProcessBy(processBacklog, PICKING, input,
                                getUnitResumeForProcess(PICKING,
                                processedUnitsLastHour))
                                .ifPresentOrElse(processes::add,
                                        () -> getProcessBy(processBacklog, PACKING, input,
                                                getUnitResumeForProcess(PACKING,
                                                processedUnitsLastHour))
                                                .ifPresentOrElse(processes::add,
                                                        () -> getProcessBy(processBacklog, WALL_IN,
                                                                input,
                                                                getUnitResumeForProcess(WALL_IN,
                                                                        processedUnitsLastHour))
                                                                .ifPresent(processes::add)
                                                )
                                )
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

    private List<Metric> createMetricsList(final ProcessBacklog processBacklog,
                                           final ProcessInfo processInfo,
                                           final GetMonitorInput input,
                                           final UnitsResume unitResume) {
        List<Metric> metrics = new ArrayList<>();
        processInfo.getMetricTypes().forEach(metricType -> {
            switch (metricType) {
                case BACKLOG:
                    metrics.add(getBacklogMetric(processBacklog, processInfo));
                    break;
                case THROUGHPUT_PER_HOUR:
                    metrics.add(getThroughputMetric(processInfo, unitResume));
                    break;
                case PRODUCTIVITY:
                    metrics.add(getProductivityMetric(input, unitResume, processInfo));
                    break;
                default:
                    break;
            }
        });
        return metrics;
    }

    private Metric getBacklogMetric(final ProcessBacklog processBacklog,
                                    final ProcessInfo processInfo) {
        final String quantity = NumberFormat.getNumberInstance(Locale.GERMAN)
                        .format(processBacklog.getQuantity());
        return createMetric(processInfo, quantity + " uds.", BACKLOG);
    }

    private UnitsResume getUnitResumeForProcess(final ProcessInfo processInfo,
                                                final List<UnitsResume> processedUnitsLastHour) {
        if (processedUnitsLastHour == null) {
            return null;
        }
        return processedUnitsLastHour.stream().filter(unitsLastHour
                -> Objects.equals(processInfo.getTitle(),
                        unitsLastHour.getProcess().getRelatedProcess()))
                .findFirst()
                .orElse(null);
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

    private List<Metric> createEmptyMetricList(ProcessInfo processInfo) {
        final Metric metric = Metric.builder()
                .type(BACKLOG.getType())
                .title(BACKLOG.getTitle())
                .subtitle(processInfo.getSubtitle())
                .value("0 uds.")
                .build();
        Map<ProcessInfo, List<Metric>> emptyMetrics = Map.of(OUTBOUND_PLANNING, List.of(metric),
                PACKING, List.of(metric,
                        createEmptyMetric(THROUGHPUT_PER_HOUR, processInfo),
                        createEmptyMetric(PRODUCTIVITY, processInfo)));
        return emptyMetrics.get(processInfo);

    }

}
