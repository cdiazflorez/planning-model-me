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
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.GetBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.GetProductivity;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.ProductivityInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.GetThroughput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.ThroughputInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent.PACKING_NO_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.CURRENT_STATUS;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
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
    private GetThroughput getThroughputMetric;

    @Mock
    private GetProductivity getProductivityMetric;

    private boolean isAnalyticsError;

    @Mock
    private OutboundWaveGateway outboundWaveGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void testExecuteOkWhenHavePutToWall() {
        // GIVEN
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .currentTime(A_DATE)
                .build();

        final ZonedDateTime cptFrom = A_DATE.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        final List<Map<String, String>> statuses = List.of(
                Map.of("status", OUTBOUND_PLANNING.getStatus()),
                Map.of("status", PACKING.getStatus())
        );

        when(backlogGateway.getBacklog(statuses, input.getWarehouseId(), cptFrom, null))
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

        commonMocks(input, cptFrom, true);

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
        assertEquals(2, outboundPlanning.getMetrics().size());

        final Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertMetric(planningBacklogMetric, OUTBOUND_PLANNING.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "0 uds.");

        final Metric outboundPlanningThroughputMetric = outboundPlanning.getMetrics().get(1);
        assertMetric(outboundPlanningThroughputMetric, "última hora",
                "Procesamiento", "throughput_per_hour", "145 uds./h");

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        final Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertMetric(pickingBacklogMetric, PICKING.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "10 uds.");

        final Metric pickingThroughputMetric = picking.getMetrics().get(1);
        assertMetric(pickingThroughputMetric, THROUGHPUT_PER_HOUR.getSubtitle(),
                THROUGHPUT_PER_HOUR.getTitle(), THROUGHPUT_PER_HOUR.getType(), "33 uds./h");

        final Metric pickingProductivityMetric = picking.getMetrics().get(2);
        assertMetric(pickingProductivityMetric, PRODUCTIVITY.getSubtitle(),
                PRODUCTIVITY.getTitle(), PRODUCTIVITY.getType(), "270 uds./h");

        final Process packing = processList.get(PACKING.getIndex());
        assertEquals(PACKING.getTitle(), packing.getTitle());
        final Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertMetric(packingBacklogMetric, PACKING.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "725 uds.");

        final Metric packingThroughputMetric = packing.getMetrics().get(1);
        assertMetric(packingThroughputMetric, THROUGHPUT_PER_HOUR.getSubtitle(),
                THROUGHPUT_PER_HOUR.getTitle(), THROUGHPUT_PER_HOUR.getType(), "20 uds./h");

        final Metric packingProductivityMetric = packing.getMetrics().get(2);
        assertMetric(packingProductivityMetric, PRODUCTIVITY.getSubtitle(),
                PRODUCTIVITY.getTitle(), PRODUCTIVITY.getType(), "145 uds./h");


        final Process packingWall = processList.get(PACKING_WALL.getIndex());
        assertEquals(PACKING_WALL.getTitle(), packingWall.getTitle());
        final Metric packingWallBacklogMetric = packingWall.getMetrics().get(0);
        assertMetric(packingWallBacklogMetric, PACKING_WALL.getSubtitle(),
                BACKLOG.getTitle(), BACKLOG.getType(), "33 uds.");

        final Process wallIn = processList.get(WALL_IN.getIndex());
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        assertEquals(1, wallIn.getMetrics().size());

        final Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertMetric(wallInBacklogMetric, WALL_IN.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "130 uds.");
    }

    @Test
    public void testExecuteOkWhenDoesntHavePutToWall() {
        // GIVEN
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .currentTime(A_DATE)
                .build();


        final ZonedDateTime cptFrom = A_DATE.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        final List<Map<String, String>> statuses = List.of(
                Map.of("status", OUTBOUND_PLANNING.getStatus()),
                Map.of("status", PACKING.getStatus())
        );

        when(backlogGateway.getBacklog(statuses, input.getWarehouseId(), cptFrom, null))
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

        commonMocks(input, cptFrom, false);

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
        assertEquals(2, outboundPlanning.getMetrics().size());

        final Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertMetric(planningBacklogMetric, OUTBOUND_PLANNING.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "0 uds.");

        final Metric outboundPlanningThroughputMetric = outboundPlanning.getMetrics().get(1);
        assertMetric(outboundPlanningThroughputMetric, "última hora",
                "Procesamiento", "throughput_per_hour", "145 uds./h");

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        final Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertMetric(pickingBacklogMetric, PICKING.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "10 uds.");

        final Metric pickingThroughputMetric = picking.getMetrics().get(1);
        assertMetric(pickingThroughputMetric, THROUGHPUT_PER_HOUR.getSubtitle(),
                THROUGHPUT_PER_HOUR.getTitle(), THROUGHPUT_PER_HOUR.getType(), "33 uds./h");

        final Metric pickingProductivityMetric = picking.getMetrics().get(2);
        assertMetric(pickingProductivityMetric, PRODUCTIVITY.getSubtitle(),
                PRODUCTIVITY.getTitle(), PRODUCTIVITY.getType(), "270 uds./h");

        final Process packing = processList.get(PACKING.getIndex() - 1);
        assertEquals(PACKING.getTitle(), packing.getTitle());
        final Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertMetric(packingBacklogMetric, PACKING.getSubtitle(), BACKLOG.getTitle(),
                BACKLOG.getType(), "725 uds.");

        final Metric packingThroughputMetric = packing.getMetrics().get(1);
        assertMetric(packingThroughputMetric, THROUGHPUT_PER_HOUR.getSubtitle(),
                THROUGHPUT_PER_HOUR.getTitle(), THROUGHPUT_PER_HOUR.getType(), "20 uds./h");

        final Metric packingProductivityMetric = packing.getMetrics().get(2);
        assertMetric(packingProductivityMetric, PRODUCTIVITY.getSubtitle(),
                PRODUCTIVITY.getTitle(), PRODUCTIVITY.getType(), "145 uds./h");
    }

    @Test
    public void testErrorOnAnalytics() {
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .currentTime(A_DATE)
                .build();

        final ZonedDateTime cptFrom = A_DATE.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        isAnalyticsError = true;

        commonMocks(input, cptFrom, true);

        when(analyticsGateway.getUnitsInInterval(WAREHOUSE_ID, 1,
                asList(AnalyticsQueryEvent.PACKING_WALL,
                        AnalyticsQueryEvent.PICKING, PACKING_NO_WALL)))
                .thenThrow(RuntimeException.class);

        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        final Optional<Process> optionalPicking = currentStatusData.getProcesses().stream()
                .filter(t -> PICKING.getTitle().equals(t.getTitle()))
                .findFirst();

        assertTrue(optionalPicking.isPresent());
        final Process picking = optionalPicking.get();
        final Metric pickingThroughputMetric = picking.getMetrics().get(1);
        assertMetric(pickingThroughputMetric, THROUGHPUT_PER_HOUR.getSubtitle(),
                "Procesamiento", "throughput_per_hour", "-");

        final Metric pickingProductivityMetric = optionalPicking.get().getMetrics().get(2);
        assertMetric(pickingProductivityMetric, PRODUCTIVITY.getSubtitle(), PRODUCTIVITY.getTitle(),
                PRODUCTIVITY.getType(), "-");

        final Optional<Process> optionalPacking = currentStatusData.getProcesses().stream()
                .filter(t -> PACKING.getTitle().equals(t.getTitle()))
                .findFirst();

        assertTrue(optionalPacking.isPresent());

        final Metric packingThroughputMetric = optionalPacking.get().getMetrics().get(1);
        assertMetric(packingThroughputMetric, THROUGHPUT_PER_HOUR.getSubtitle(),
                THROUGHPUT_PER_HOUR.getTitle(), THROUGHPUT_PER_HOUR.getType(), "-");

        final Metric packingProductivityMetric = optionalPacking.get().getMetrics().get(2);
        assertMetric(packingProductivityMetric, PRODUCTIVITY.getSubtitle(),
                PRODUCTIVITY.getTitle(), PRODUCTIVITY.getType(), "-");
    }

    private void commonMocks(final GetCurrentStatusInput input,
                             final ZonedDateTime cptFrom,
                             final boolean hasPutToWall) {

        when(planningModelGateway.getEntities(any(EntityRequest.class)))
                .thenReturn(mockHeadcountEntities(getCurrentUtcDate()));

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
                null,
                null)))
                .thenReturn(pickingProcessBacklog);

        if (hasPutToWall) {
            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(
                            WALL_IN.getStatus(), input.getWarehouseId(), cptFrom, null, null)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(WALL_IN.getStatus())
                            .quantity(725)
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(
                            PACKING.getStatus(), input.getWarehouseId(), cptFrom, null, "PW")))
                    .thenReturn(ProcessBacklog.builder()
                            .process(PACKING.getStatus())
                            .quantity(725)
                            .area("PW")
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(
                            WALL_IN.getStatus(), input.getWarehouseId(), cptFrom, null, null)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(WALL_IN.getStatus())
                            .quantity(725)
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(
                            PACKING_WALL.getStatus(), input.getWarehouseId(), cptFrom, null, "PW")))
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
                "ORDER"))
                .thenReturn(UnitsResume.builder()
                        .unitCount(54)
                        .build());

        when(logisticCenterGateway.getConfiguration(input.getWarehouseId()))
                .thenReturn(new LogisticCenterConfiguration(getTimeZone(UTC), hasPutToWall));

        mockGetBacklogMetric();
        mockGetProductivityMetric();
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

    private List<Entity> mockHeadcountEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(ProcessName.PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(ProcessName.PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(ProcessName.PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                Entity.builder()
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
                return createMetric(BACKLOG, OUTBOUND_PLANNING, "0 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(PICKING)) {
                return createMetric(BACKLOG, PICKING, "10 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(PACKING_WALL)) {
                return createMetric(BACKLOG, PACKING_WALL, "33 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(PACKING)) {
                return createMetric(BACKLOG, PACKING, "725 uds.");
            } else if (backlogMetricInput.getProcessInfo().equals(WALL_IN)) {
                return createMetric(BACKLOG, WALL_IN, "130 uds.");
            } else {
                throw new IllegalArgumentException();
            }
        });
    }

    private void mockGetProductivityMetric() {
        when(getProductivityMetric.execute(any(ProductivityInput.class))).thenAnswer(invocation -> {
            ProductivityInput productivityInput = invocation.getArgument(0);
            switch (productivityInput.getProcessInfo()) {
                case  OUTBOUND_PLANNING:
                    return createMetric(PRODUCTIVITY, OUTBOUND_PLANNING, "20 uds./h");
                case PICKING:
                    return createMetric(PRODUCTIVITY, PICKING, "270 uds./h");
                case PACKING_WALL:
                    return createMetric(PRODUCTIVITY, PACKING_WALL, "33 uds./h");
                case PACKING:
                    return createMetric(PRODUCTIVITY, PACKING, "145 uds./h");
                case WALL_IN:
                    return createMetric(PRODUCTIVITY, WALL_IN, "176 uds./h");
                default:
                    throw new IllegalArgumentException();
            }
        });
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

        final String subtitle = metricType.equals(BACKLOG)
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
