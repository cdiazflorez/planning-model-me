package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.GetBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.immediatebacklog.get.GetImmediateBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.GetThroughput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.ThroughputInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_NO_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.CURRENT_STATUS;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.ORDER_GROUP_TYPE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.TimeZone.getTimeZone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentStatusTest {

    @InjectMocks
    private GetCurrentStatus getCurrentStatus;

    @Mock
    private AnalyticsGateway analyticsGateway;

    @Mock
    private BacklogGateway backlogGateway;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private GetBacklogMetricUseCase getBacklogMetric;

    @Mock
    private GetImmediateBacklogMetricUseCase getImmediateBacklogMetricUseCase;

    @Mock
    private GetThroughput getThroughputMetric;

    private boolean isAnalyticsError;

    @Mock
    private OutboundWaveGateway outboundWaveGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void testExecuteOkWhenHavePutToWall() {
        // GIVEN
        final ZonedDateTime currentDate = getCurrentUtcDate();
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(currentDate)
                .dateTo(currentDate.plusHours(25))
                .currentTime(currentDate)
                .groupType(ORDER_GROUP_TYPE)
                .build();

        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        final List<Map<String, String>> statuses = List.of(
                Map.of("status", OUTBOUND_PLANNING.getStatus()),
                Map.of("status", PACKING.getStatus())
        );

        when(backlogGateway.getBacklog(statuses, input.getWarehouseId(), cptFrom, cptTo, false))
                .thenReturn(
                        new ArrayList<>(
                                List.of(
                                        ProcessBacklog.builder()
                                                .process(OUTBOUND_PLANNING.getStatus())
                                                .quantity(1442)
                                                .build(),
                                        ProcessBacklog.builder()
                                                .process(PACKING.getStatus())
                                                .quantity(1442)
                                                .build())));

        commonMocks(input, cptFrom, cptTo, currentDate, true);

        when(analyticsGateway.getUnitsInInterval(WAREHOUSE_ID, 1,
                asList(AnalyticsQueryEvent.PACKING_WALL,
                        AnalyticsQueryEvent.PICKING,
                        PACKING_NO_WALL)))
                .thenReturn(List.of(
                        new UnitsResume(150, 120, AnalyticsQueryEvent.PACKING_WALL),
                        new UnitsResume(3020, 2020, AnalyticsQueryEvent.PICKING),
                        new UnitsResume(2030, 102, PACKING_NO_WALL)));

        // WHEN
        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        // THEN
        final TreeSet<Process> processes = currentStatusData.getProcesses();
        assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
        assertEquals(5, processes.size());

        List<Process> processList = new ArrayList<>(currentStatusData.getProcesses());

        final Process outboundPlanning = processList.get(OUTBOUND_PLANNING.getIndex());
        assertEquals(OUTBOUND_PLANNING.getTitle(), outboundPlanning.getTitle());
        assertEquals(3, outboundPlanning.getMetrics().size());

        final Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertMetric(planningBacklogMetric, OUTBOUND_PLANNING.getSubtitle(),
                TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "0 uds.");

        final Metric planningImmediateBacklogMetric = outboundPlanning.getMetrics().get(1);
        assertMetric(planningImmediateBacklogMetric, null,
                IMMEDIATE_BACKLOG.getTitle(), IMMEDIATE_BACKLOG.getType(), "10 uds.");

        final Metric outboundPlanningThroughputMetric = outboundPlanning.getMetrics().get(2);
        assertMetric(outboundPlanningThroughputMetric, "última hora",
                "Procesamiento", "throughput_per_hour", "145 uds./h");

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        final Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertMetric(pickingBacklogMetric, PICKING.getSubtitle(), TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "10 uds.");

        final Process packing = processList.get(PACKING.getIndex());
        assertEquals(PACKING.getTitle(), packing.getTitle());
        final Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertMetric(packingBacklogMetric, PACKING.getSubtitle(), TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "725 uds.");

        final Process packingWall = processList.get(PACKING_WALL.getIndex());
        assertEquals(PACKING_WALL.getTitle(), packingWall.getTitle());
        final Metric packingWallBacklogMetric = packingWall.getMetrics().get(0);
        assertMetric(packingWallBacklogMetric, PACKING_WALL.getSubtitle(),
                TOTAL_BACKLOG.getTitle(), TOTAL_BACKLOG.getType(), "33 uds.");

        final Process wallIn = processList.get(WALL_IN.getIndex());
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        assertEquals(1, wallIn.getMetrics().size());

        final Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertMetric(wallInBacklogMetric, WALL_IN.getSubtitle(), TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "130 uds.");
    }

    @Test
    public void testExecuteOkWhenDoesntHavePutToWall() {
        // GIVEN
        final ZonedDateTime currentDate = getCurrentUtcDate();
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(currentDate)
                .dateTo(currentDate.plusHours(25))
                .currentTime(currentDate)
                .groupType(ORDER_GROUP_TYPE)
                .build();


        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        final List<Map<String, String>> statuses = List.of(
                Map.of("status", OUTBOUND_PLANNING.getStatus()),
                Map.of("status", PACKING.getStatus())
        );

        when(backlogGateway.getBacklog(statuses, input.getWarehouseId(), cptFrom, cptTo, false))
                .thenReturn(
                        new ArrayList<>(
                                List.of(
                                        ProcessBacklog.builder()
                                                .process(OUTBOUND_PLANNING.getStatus())
                                                .quantity(1442)
                                                .build(),
                                        ProcessBacklog.builder()
                                                .process(PACKING.getStatus())
                                                .quantity(1442)
                                                .build())));

        commonMocks(input, cptFrom, cptTo, currentDate,false);

        when(analyticsGateway.getUnitsInInterval(WAREHOUSE_ID, 1,
                singletonList(AnalyticsQueryEvent.PICKING)))
                .thenReturn(
                        singletonList(new UnitsResume(3020, 2020, AnalyticsQueryEvent.PICKING)));

        // WHEN
        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        // THEN
        final TreeSet<Process> processes = currentStatusData.getProcesses();
        assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
        assertEquals(3, processes.size());

        List<Process> processList = new ArrayList<>(currentStatusData.getProcesses());

        final Process outboundPlanning = processList.get(OUTBOUND_PLANNING.getIndex());
        assertEquals(OUTBOUND_PLANNING.getTitle(), outboundPlanning.getTitle());
        assertEquals(3, outboundPlanning.getMetrics().size());

        final Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertMetric(planningBacklogMetric, OUTBOUND_PLANNING.getSubtitle(),
                TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "0 uds.");

        final Metric planningImmediateBacklogMetric = outboundPlanning.getMetrics().get(1);
        assertMetric(planningImmediateBacklogMetric, null,
                IMMEDIATE_BACKLOG.getTitle(), IMMEDIATE_BACKLOG.getType(), "10 uds.");

        final Metric outboundPlanningThroughputMetric = outboundPlanning.getMetrics().get(2);
        assertMetric(outboundPlanningThroughputMetric, "última hora",
                "Procesamiento", "throughput_per_hour", "145 uds./h");

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        final Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertMetric(pickingBacklogMetric, PICKING.getSubtitle(), TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "10 uds.");

        final Process packing = processList.get(PACKING.getIndex() - 1);
        assertEquals(PACKING.getTitle(), packing.getTitle());
        final Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertMetric(packingBacklogMetric, PACKING.getSubtitle(), TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "725 uds.");
    }

    @Test
    public void testErrorOnAnalytics() {
        final ZonedDateTime currentDate = getCurrentUtcDate();
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(currentDate)
                .dateTo(currentDate.plusHours(25))
                .currentTime(currentDate)
                .groupType(ORDER_GROUP_TYPE)
                .build();

        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = true;

        commonMocks(input, cptFrom, cptTo, currentDate, true);

        when(analyticsGateway.getUnitsInInterval(WAREHOUSE_ID, 1,
                asList(AnalyticsQueryEvent.PACKING_WALL,
                        AnalyticsQueryEvent.PICKING, PACKING_NO_WALL)))
                .thenThrow(RuntimeException.class);

        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        final Optional<Process> optionalPicking = currentStatusData.getProcesses().stream()
                .filter(t -> PICKING.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalPicking.isPresent());

        final Optional<Process> optionalPacking = currentStatusData.getProcesses().stream()
                .filter(t -> PACKING.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalPacking.isPresent());
    }

    private void commonMocks(final GetCurrentStatusInput input,
                             final ZonedDateTime cptFrom,
                             final ZonedDateTime cptTo,
                             final ZonedDateTime currentDate,
                             final boolean hasPutToWall) {

        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.of(backlogGateway));

        final ProcessBacklog pickingProcessBacklog = ProcessBacklog.builder()
                .process(PICKING.getStatus())
                .quantity(725)
                .build();

        when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                PICKING.getStatus(),
                input.getWarehouseId(),
                cptFrom,
                cptTo,
                null,
                ORDER_GROUP_TYPE,
                false)))
                .thenReturn(pickingProcessBacklog);

        when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                OUTBOUND_PLANNING.getStatus(),
                input.getWarehouseId(),
                currentDate.minusDays(1),
                currentDate.plusDays(1),
                null,
                ORDER_GROUP_TYPE,
                false)))
                .thenReturn(ProcessBacklog.builder()
                        .process(OUTBOUND_PLANNING.getStatus())
                        .immediateQuantity(200)
                        .build());

        if (hasPutToWall) {
            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, null, ORDER_GROUP_TYPE, false)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(WALL_IN.getStatus())
                            .quantity(725)
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(PACKING.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, "PW", ORDER_GROUP_TYPE, false)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(PACKING.getStatus())
                            .quantity(725)
                            .area("PW")
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, null, ORDER_GROUP_TYPE, false)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(WALL_IN.getStatus())
                            .quantity(725)
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(PACKING_WALL.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, "PW", ORDER_GROUP_TYPE, false)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(PACKING.getStatus())
                            .quantity(725)
                            .area("PW")
                            .build());
        }

        final ZonedDateTime currentTime = ZonedDateTime.now(UTC).withSecond(0).withNano(0);

        when(outboundWaveGateway.getUnitsCount(
                input.getWarehouseId(),
                currentTime.minusHours(1),
                currentTime,
                ORDER_GROUP_TYPE))
                .thenReturn(UnitsResume.builder()
                        .unitCount(54)
                        .build());

        when(logisticCenterGateway.getConfiguration(input.getWarehouseId()))
                .thenReturn(new LogisticCenterConfiguration(getTimeZone(UTC), hasPutToWall));

        mockGetBacklogMetric();
        mockGetImmediateBacklogMetric();
        mockGetThroughputMetric();
    }

    private void assertMetric(final Metric metric, final String expectedSubtitle,
                              final String expectedTitle, final String expectedType,
                              final String expectedValue) {
        assertEquals(expectedSubtitle, metric.getSubtitle());
        assertEquals(expectedTitle, metric.getTitle());
        assertEquals(expectedType, metric.getType());
        assertEquals(expectedValue, metric.getValue());
    }

    private List<MagnitudePhoto> mockHeadcountEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                MagnitudePhoto.builder()
                        .date(utcCurrentTime)
                        .processName(ProcessName.PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime)
                        .processName(ProcessName.PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(ProcessName.PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(ProcessName.PICKING)
                        .source(Source.FORECAST)
                        .value(30)
                        .build()
        );
    }

    private void mockGetBacklogMetric() {
        when(getBacklogMetric.execute(any(BacklogMetricInput.class))).thenAnswer(invocation -> {
            BacklogMetricInput backlogMetricInput = invocation.getArgument(0);
            if (backlogMetricInput.getProcessInfo().equals(OUTBOUND_PLANNING)) {
                return createMetric(TOTAL_BACKLOG, OUTBOUND_PLANNING, "0 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(PICKING)) {
                return createMetric(TOTAL_BACKLOG, PICKING, "10 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(PACKING_WALL)) {
                return createMetric(TOTAL_BACKLOG, PACKING_WALL, "33 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(PACKING)) {
                return createMetric(TOTAL_BACKLOG, PACKING, "725 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(WALL_IN)) {
                return createMetric(TOTAL_BACKLOG, WALL_IN, "130 uds.");
            } else {
                throw new IllegalArgumentException();
            }
        });
    }

    private void mockGetImmediateBacklogMetric() {
        when(getImmediateBacklogMetricUseCase.execute(any(BacklogMetricInput.class)))
                .thenReturn(createMetric(IMMEDIATE_BACKLOG, OUTBOUND_PLANNING, "10 uds."));
    }

    private void mockGetThroughputMetric() {
        when(getThroughputMetric.execute(any(ThroughputInput.class))).thenAnswer(invocation -> {
            ThroughputInput throughputInput = invocation.getArgument(0);
            switch (throughputInput.getProcessInfo()) {
                case OUTBOUND_PLANNING:
                    return createMetric(THROUGHPUT_PER_HOUR, OUTBOUND_PLANNING, "145 uds./h");
                case PICKING:
                    return createMetric(THROUGHPUT_PER_HOUR, PICKING, "33 uds./h");
                case PACKING_WALL:
                    return createMetric(THROUGHPUT_PER_HOUR, PACKING_WALL, "176 uds./h");
                case PACKING:
                    return createMetric(THROUGHPUT_PER_HOUR, PACKING, "20 uds./h");
                case WALL_IN:
                    return createMetric(THROUGHPUT_PER_HOUR, WALL_IN, "270 uds./h");
                default:
                    throw new IllegalArgumentException();
            }
        });
    }

    private Metric createMetric(final MetricType metricType,
                                final ProcessInfo processInfo,
                                final String value) {

        final String subtitle = metricType.equals(TOTAL_BACKLOG)
                ? processInfo.getSubtitle()
                : metricType.getSubtitle();

        final String finalValue = isAnalyticsError
                && (metricType.equals(THROUGHPUT_PER_HOUR)
                || metricType.equals(PRODUCTIVITY)) ? "-" : value;

        return Metric.builder().title(metricType.getTitle())
                .type(metricType.getType())
                .subtitle(subtitle)
                .value(finalValue)
                .build();
    }
}
