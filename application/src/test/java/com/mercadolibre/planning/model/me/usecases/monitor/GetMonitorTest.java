package com.mercadolibre.planning.model.me.usecases.monitor;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.outboundwave.OutboundWaveGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get.GetCurrentStatus;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;

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
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMonitorTest {

    private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

    @InjectMocks
    private GetMonitor getMonitor;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetSales getSales;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private GetCurrentStatus getCurrentStatus;

    private static final TimeZone TIME_ZONE = getDefault();

    @Test
    public void testExecuteOk() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
        final GetMonitorInput input = GetMonitorInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .build();

        commonMocks(utcCurrentTime, input);

        // WHEN
        final Monitor monitor = getMonitor.execute(input);

        // THEN
        assertEquals("Modelo de Priorización", monitor.getTitle());
        assertEquals("Estado Actual", monitor.getSubtitle1());
        assertEquals("Última actualización: Hoy - " + getCurrentTime(), monitor.getSubtitle2());

        final List<MonitorData> monitorDataList = monitor.getMonitorData();
        assertEquals(2, monitorDataList.size());

        final CurrentStatusData currentStatusData = (CurrentStatusData) monitorDataList.get(1);
        final TreeSet<Process> processes = currentStatusData.getProcesses();
        assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
        assertEquals(5, processes.size());

        List<Process> processList = new ArrayList<>(currentStatusData.getProcesses());

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertEquals(PICKING.getSubtitle(), pickingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), pickingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), pickingBacklogMetric.getType());
        assertEquals("2.232 uds.", pickingBacklogMetric.getValue());

        Metric pickingThroughputMetric = picking.getMetrics().get(1);
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), pickingThroughputMetric.getSubtitle());
        assertEquals(THROUGHPUT_PER_HOUR.getTitle(), pickingThroughputMetric.getTitle());
        assertEquals(THROUGHPUT_PER_HOUR.getType(), pickingThroughputMetric.getType());
        assertEquals("3020 uds./h", pickingThroughputMetric.getValue());

        Metric pickingProductivityMetric = picking.getMetrics().get(2);
        assertEquals(PRODUCTIVITY.getSubtitle(), pickingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), pickingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), pickingProductivityMetric.getType());
        assertEquals("53 uds./h", pickingProductivityMetric.getValue());


        final Process packing = processList.get(PACKING.getIndex());
        assertEquals(PACKING.getTitle(), packing.getTitle());
        Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertEquals(PACKING.getSubtitle(), packingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), packingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), packingBacklogMetric.getType());
        assertEquals("1.442 uds.", packingBacklogMetric.getValue());

        Metric packingThroughputMetric = packing.getMetrics().get(1);
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), packingThroughputMetric.getSubtitle());
        assertEquals(THROUGHPUT_PER_HOUR.getTitle(), packingThroughputMetric.getTitle());
        assertEquals(THROUGHPUT_PER_HOUR.getType(), packingThroughputMetric.getType());
        assertEquals("150 uds./h", packingThroughputMetric.getValue());

        Metric packingProductivityMetric = packing.getMetrics().get(2);
        assertEquals(PRODUCTIVITY.getSubtitle(), packingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), packingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), packingProductivityMetric.getType());

        final Process packingWall = processList.get(PACKING_WALL.getIndex());
        assertEquals(PACKING_WALL.getTitle(), packingWall.getTitle());
        Metric packingWallBacklogMetric = packingWall.getMetrics().get(0);
        assertEquals(PACKING_WALL.getSubtitle(), packingWallBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), packingWallBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), packingWallBacklogMetric.getType());
        assertEquals("981 uds.", packingWallBacklogMetric.getValue());

        final Process wallIn = processList.get(WALL_IN.getIndex());
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertEquals(WALL_IN.getSubtitle(), wallInBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), wallInBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), wallInBacklogMetric.getType());
        assertEquals("725 uds.", wallInBacklogMetric.getValue());

        final Process outboundPlanning = processList.get(OUTBOUND_PLANNING.getIndex());
        assertEquals(OUTBOUND_PLANNING.getTitle(), outboundPlanning.getTitle());
        Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertEquals(OUTBOUND_PLANNING.getSubtitle(), planningBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), planningBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), planningBacklogMetric.getType());
        assertEquals("0 uds.", planningBacklogMetric.getValue());

        assertTrue(monitorDataList.get(0) instanceof DeviationData);
        DeviationData deviationData = (DeviationData) monitorDataList.get(0);
        assertEquals("-13.15%", deviationData.getMetrics().getDeviationPercentage()
                .getValue());
        assertNull(deviationData.getMetrics().getDeviationPercentage().getStatus());
        assertEquals("arrow_down", deviationData.getMetrics().getDeviationPercentage()
                .getIcon());
        assertEquals("905 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getCurrentUnits().getValue());
        assertEquals("1042 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getForecastUnits().getValue());

    }

    private void commonMocks(final ZonedDateTime utcCurrentTime, final GetMonitorInput input) {
        when(getCurrentStatus.execute(input))
                .thenReturn(CurrentStatusData.builder().processes(
                        new TreeSet<>(List.of(
                                Process.builder().title(OUTBOUND_PLANNING.getTitle())
                                        .metrics(
                                                List.of(createMetric(BACKLOG.getTitle(),
                                                        BACKLOG.getType(),
                                                        OUTBOUND_PLANNING.getSubtitle(),
                                                        "0 uds.")
                                                )
                                        ).build(),
                                Process.builder().title(PICKING.getTitle())
                                        .metrics(
                                                List.of(
                                                        createMetric(BACKLOG.getTitle(),
                                                                BACKLOG.getType(),
                                                                PICKING.getSubtitle(),
                                                                "2.232 uds."),
                                                        createMetric(THROUGHPUT_PER_HOUR.getTitle(),
                                                                THROUGHPUT_PER_HOUR.getType(),
                                                                THROUGHPUT_PER_HOUR.getSubtitle(),
                                                                "3020 uds./h"),
                                                        createMetric(PRODUCTIVITY.getTitle(),
                                                                PRODUCTIVITY.getType(),
                                                                PRODUCTIVITY.getSubtitle(),
                                                                "53 uds./h")
                                                )
                                        ).build(),
                                Process.builder().title(PACKING.getTitle())
                                        .metrics(
                                                List.of(
                                                        createMetric(BACKLOG.getTitle(),
                                                                BACKLOG.getType(),
                                                                PACKING.getSubtitle(),
                                                                "1.442 uds."),
                                                        createMetric(THROUGHPUT_PER_HOUR.getTitle(),
                                                                THROUGHPUT_PER_HOUR.getType(),
                                                                THROUGHPUT_PER_HOUR.getSubtitle(),
                                                                "150 uds./h"),
                                                        createMetric(PRODUCTIVITY.getTitle(),
                                                                PRODUCTIVITY.getType(),
                                                                PRODUCTIVITY.getSubtitle(),
                                                                "53 uds./h")
                                                )
                                        ).build(),
                                Process.builder().title(PACKING_WALL.getTitle())
                                        .metrics(
                                                List.of(createMetric(BACKLOG.getTitle(),
                                                        BACKLOG.getType(),
                                                        PACKING_WALL.getSubtitle(),
                                                        "981 uds.")
                                        )
                                        ).build(),
                                Process.builder().title(WALL_IN.getTitle())
                                        .metrics(
                                                List.of(createMetric(BACKLOG.getTitle(),
                                                        BACKLOG.getType(),
                                                        WALL_IN.getSubtitle(),
                                                        "725 uds.")
                                                )
                                        ).build()
                        ))
                ).build());

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(getSales.execute(new GetSalesInputDto(
                FBM_WMS_OUTBOUND, WAREHOUSE_ID, utcCurrentTime.minusHours(28)))
        ).thenReturn(mockSales());
        
        when(planningModelGateway.getPlanningDistribution(Mockito.any()
        )).thenReturn(mockPlanningDistribution(utcCurrentTime));
    }

    private Metric createMetric(final String title, final String type, final String subtitle,
                                final String value) {
        return Metric.builder().title(title)
                .type(type)
                .subtitle(subtitle)
                .value(value)
                .build();
    }
    
    private List<Backlog> mockSales() {
        return List.of(
                Backlog.builder()
                        .date(CPT_1)
                        .quantity(350)
                        .build(),
                Backlog.builder()
                        .date(CPT_2)
                        .quantity(235)
                        .build(),
                Backlog.builder()
                        .date(CPT_3)
                        .quantity(200)
                        .build(),
                Backlog.builder()
                        .date(CPT_4)
                        .quantity(120)
                        .build()
        );
    }
    
    private List<PlanningDistributionResponse> mockPlanningDistribution(
            final ZonedDateTime utcCurrentTime) {
        return List.of(
                new PlanningDistributionResponse(utcCurrentTime, CPT_1, MetricUnit.UNITS, 281),
                new PlanningDistributionResponse(utcCurrentTime, CPT_1, MetricUnit.UNITS, 128),
                new PlanningDistributionResponse(utcCurrentTime, CPT_2, MetricUnit.UNITS, 200),
                new PlanningDistributionResponse(utcCurrentTime, CPT_3, MetricUnit.UNITS, 207),
                new PlanningDistributionResponse(utcCurrentTime, CPT_4, MetricUnit.UNITS, 44),
                new PlanningDistributionResponse(utcCurrentTime, CPT_4, MetricUnit.UNITS, 82),
                new PlanningDistributionResponse(utcCurrentTime, CPT_5, MetricUnit.UNITS, 100)
        );
    }

    @Test
    public void testExecuteError() {
        // GIVEN
        final GetMonitorInput input = GetMonitorInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(getCurrentUtcDate())
                .dateTo(getCurrentUtcDate().plusHours(25))
                .build();

        when(getCurrentStatus.execute(input)).thenThrow(BacklogGatewayNotSupportedException.class);

        // WHEN
        assertThrows(BacklogGatewayNotSupportedException.class, () -> getMonitor.execute(input));

        // THEN
    }

    private String getCurrentTime() {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime currentDate = convertToTimeZone(TIME_ZONE.toZoneId(), now);
        final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("HH:mm");
        return currentDate.format(formatter2);
    }
}
