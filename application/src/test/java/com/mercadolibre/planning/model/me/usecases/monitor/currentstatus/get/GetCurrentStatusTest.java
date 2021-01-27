package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.analytics.AnalyticsGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
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
import com.mercadolibre.planning.model.me.utils.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    public void testExecuteOk() {
        // GIVEN
        final ZonedDateTime dateFromForBacklogs = DateUtils.getCurrentUtcDateTime().minusDays(7);
        final GetMonitorInput input = GetMonitorInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .build();
        isAnalyticsError = false;
        final String status = "status";
        final List<Map<String, String>> statuses = List.of(
                Map.of(status, OUTBOUND_PLANNING.getStatus()),
                Map.of(status, PACKING.getStatus())
        );
        when(backlogGateway.getBacklog(statuses,
                input.getWarehouseId(),
                dateFromForBacklogs,
                null
        )).thenReturn(
                new ArrayList<>(
                        List.of(
                                ProcessBacklog.builder()
                                        .process(OUTBOUND_PLANNING.getStatus())
                                        .quantity(1442)
                                        .build(),
                                ProcessBacklog.builder()
                                        .process(PACKING.getStatus())
                                        .quantity(1442)
                                        .build()
                        ))
        );
        commonMocks(input, dateFromForBacklogs);

        when(analyticsGateway.getUnitsInInterval(WAREHOUSE_ID, 1,
                Arrays.asList(AnalyticsQueryEvent.PACKING_WALL, AnalyticsQueryEvent.PICKING,
                        PACKING_NO_WALL)
        )).thenReturn(List.of(
                UnitsResume.builder()
                        .process(AnalyticsQueryEvent.PACKING_WALL)
                        .eventCount(120)
                        .unitCount(150)
                        .build(),
                UnitsResume.builder()
                        .process(AnalyticsQueryEvent.PICKING)
                        .eventCount(2020)
                        .unitCount(3020)
                        .build(),
                UnitsResume.builder()
                        .process(PACKING_NO_WALL)
                        .eventCount(102)
                        .unitCount(2030)
                        .build())
        );

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
        assertEquals(OUTBOUND_PLANNING.getSubtitle(), planningBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), planningBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), planningBacklogMetric.getType());
        assertEquals("0 uds.", planningBacklogMetric.getValue());

        final Metric outboundPlanningThroughputMetric = outboundPlanning.getMetrics().get(1);
        assertEquals("última hora",
                outboundPlanningThroughputMetric.getSubtitle());
        assertEquals("Procesamiento", outboundPlanningThroughputMetric.getTitle());
        assertEquals("throughput_per_hour", outboundPlanningThroughputMetric.getType());
        assertEquals("145 uds./h", outboundPlanningThroughputMetric.getValue());

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        final Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertEquals(PICKING.getSubtitle(), pickingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), pickingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), pickingBacklogMetric.getType());
        assertEquals("10 uds.", pickingBacklogMetric.getValue());

        final Metric pickingThroughputMetric = picking.getMetrics().get(1);
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), pickingThroughputMetric.getSubtitle());
        assertEquals(THROUGHPUT_PER_HOUR.getTitle(), pickingThroughputMetric.getTitle());
        assertEquals(THROUGHPUT_PER_HOUR.getType(), pickingThroughputMetric.getType());
        assertEquals("33 uds./h", pickingThroughputMetric.getValue());

        final Metric pickingProductivityMetric = picking.getMetrics().get(2);
        assertEquals(PRODUCTIVITY.getSubtitle(), pickingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), pickingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), pickingProductivityMetric.getType());
        assertEquals("270 uds./h", pickingProductivityMetric.getValue());

        final Process packing = processList.get(PACKING.getIndex());
        assertEquals(PACKING.getTitle(), packing.getTitle());
        final Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertEquals(PACKING.getSubtitle(), packingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), packingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), packingBacklogMetric.getType());
        assertEquals("725 uds.", packingBacklogMetric.getValue());

        final Metric packingThroughputMetric = packing.getMetrics().get(1);
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), packingThroughputMetric.getSubtitle());
        assertEquals(THROUGHPUT_PER_HOUR.getTitle(), packingThroughputMetric.getTitle());
        assertEquals(THROUGHPUT_PER_HOUR.getType(), packingThroughputMetric.getType());
        assertEquals("20 uds./h", packingThroughputMetric.getValue());

        final Metric packingProductivityMetric = packing.getMetrics().get(2);
        assertEquals(PRODUCTIVITY.getSubtitle(), packingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), packingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), packingProductivityMetric.getType());
        assertEquals("145 uds./h", packingProductivityMetric.getValue());


        final Process packingWall = processList.get(PACKING_WALL.getIndex());
        assertEquals(PACKING_WALL.getTitle(), packingWall.getTitle());
        final Metric packingWallBacklogMetric = packingWall.getMetrics().get(0);
        assertEquals(PACKING_WALL.getSubtitle(), packingWallBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), packingWallBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), packingWallBacklogMetric.getType());
        assertEquals("33 uds.", packingWallBacklogMetric.getValue());

        final Process wallIn = processList.get(WALL_IN.getIndex());
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        assertEquals(1, wallIn.getMetrics().size());

        final Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertEquals(WALL_IN.getSubtitle(), wallInBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), wallInBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), wallInBacklogMetric.getType());
        assertEquals("130 uds.", wallInBacklogMetric.getValue());
    }

    @Test
    public void testErrorOnAnalytics() {
        final ZonedDateTime dateFromForBacklogs = DateUtils.getCurrentUtcDateTime().minusDays(7);
        final GetMonitorInput input = GetMonitorInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .build();
        isAnalyticsError = true;
        commonMocks(input, dateFromForBacklogs);
        when(analyticsGateway.getUnitsInInterval(WAREHOUSE_ID, 1,
                Arrays.asList(AnalyticsQueryEvent.PACKING_WALL, AnalyticsQueryEvent.PICKING,
                        PACKING_NO_WALL)
        )).thenThrow(RuntimeException.class);

        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        final Optional<Process> optionalPicking = currentStatusData.getProcesses().stream()
                .filter(t -> PICKING.getTitle().equals(t.getTitle()))
                .findFirst();
        final Process picking = optionalPicking.get();
        final Metric pickingThroughputMetric = picking.getMetrics().get(1);
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), pickingThroughputMetric.getSubtitle());
        assertEquals("Procesamiento", pickingThroughputMetric.getTitle());
        assertEquals("throughput_per_hour", pickingThroughputMetric.getType());
        assertEquals("-", pickingThroughputMetric.getValue());

        final Metric pickingProductivityMetric = picking.getMetrics().get(2);
        assertEquals(PRODUCTIVITY.getSubtitle(), pickingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), pickingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), pickingProductivityMetric.getType());
        assertEquals("-", pickingProductivityMetric.getValue());

        final Optional<Process> optionalPacking = currentStatusData.getProcesses().stream()
                .filter(t -> PACKING.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalPacking.isPresent());
        final Process packing = optionalPacking.get();

        final Metric packingThroughputMetric = packing.getMetrics().get(1);
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), packingThroughputMetric.getSubtitle());
        assertEquals(THROUGHPUT_PER_HOUR.getTitle(), packingThroughputMetric.getTitle());
        assertEquals(THROUGHPUT_PER_HOUR.getType(), packingThroughputMetric.getType());
        assertEquals("-", packingThroughputMetric.getValue());

        final Metric packingProductivityMetric = packing.getMetrics().get(2);
        assertEquals(PRODUCTIVITY.getSubtitle(), packingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), packingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), packingProductivityMetric.getType());
        assertEquals("-", packingProductivityMetric.getValue());
    }

    private void commonMocks(final GetMonitorInput input,
                             final ZonedDateTime dateFromForBacklogs) {
        when(
                planningModelGateway.getEntities(Mockito.any())
        ).thenReturn(mockHeadcountEntities(getCurrentUtcDate()));
        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.of(backlogGateway));

        final ProcessBacklog pickingProcessBacklog = ProcessBacklog.builder()
                .process(PICKING.getStatus())
                .quantity(725)
                .build();
        when(backlogGateway.getUnitBacklog(PICKING.getStatus(),
                input.getWarehouseId(),
                dateFromForBacklogs,
                null, null
        )).thenReturn(
                pickingProcessBacklog
        );

        when(backlogGateway.getUnitBacklog(WALL_IN.getStatus(),
                input.getWarehouseId(),
                dateFromForBacklogs,
                null, null
        )).thenReturn(
                ProcessBacklog.builder()
                        .process(WALL_IN.getStatus())
                        .quantity(725)
                        .build()
        );

        when(backlogGateway.getUnitBacklog(PACKING.getStatus(),
                input.getWarehouseId(),
                dateFromForBacklogs,
                null, "PW"
        )).thenReturn(
                ProcessBacklog.builder()
                        .process(PACKING.getStatus())
                        .quantity(725)
                        .area("PW")
                        .build()
        );

        final ZonedDateTime currentTime = ZonedDateTime.now(UTC).withSecond(0).withNano(0);

        when(outboundWaveGateway.getUnitsCount(
                input.getWarehouseId(),
                currentTime.minusHours(1),
                currentTime,
                "ORDER"
        )).thenReturn(UnitsResume.builder().unitCount(54).build());

        mockGetBacklogMetric();
        mockGetProductivityMetric();
        mockGetThroughputMetric();
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
        final String subtitle = metricType.equals(BACKLOG) ? processInfo.getSubtitle() :
                metricType.getSubtitle();
        final String finalValue = isAnalyticsError && (metricType.equals(THROUGHPUT_PER_HOUR)
                || metricType.equals(PRODUCTIVITY)) ? "-" : value;
        return Metric.builder().title(metricType.getTitle())
                .type(metricType.getType())
                .subtitle(subtitle)
                .value(finalValue)
                .build();
    }
}
