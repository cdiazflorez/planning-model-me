package com.mercadolibre.planning.model.me.usecases.currentstatus;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
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
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorDataType.CURRENT_STATUS;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
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
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private BacklogGateway backlogGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;
    
    @Mock
    private GetSales getSales;

    @Mock
    private PlanningModelGateway planningModelGateway;

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

        final String status = "status";
        List<Map<String, String>> statuses = List.of(
                Map.of(status, OUTBOUND_PLANNING.getStatus()),
                Map.of(status, PACKING.getStatus())
        );

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.of(backlogGateway));

        when(backlogGateway.getBacklog(statuses,
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo()
        )).thenReturn(
                new ArrayList<>(
                        List.of(
                                ProcessBacklog.builder()
                                        .process(PACKING.getStatus())
                                        .quantity(1442)
                                        .build()
                        ))
        );
        when(backlogGateway.getUnitBacklog(PICKING.getStatus(),
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo()
        )).thenReturn(
                ProcessBacklog.builder()
                        .process(WALL_IN.getStatus())
                        .quantity(725)
                        .build()
        );
        
        when(getSales.execute(new GetSalesInputDto(
                FBM_WMS_OUTBOUND, WAREHOUSE_ID, utcCurrentTime.minusHours(28)))
        ).thenReturn(mockSales());
        
        when(planningModelGateway.getPlanningDistribution(Mockito.any()
        )).thenReturn(mockPlanningDistribution(utcCurrentTime));
        when(backlogGateway.getUnitBacklog(WALL_IN.getStatus(),
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo()
        )).thenReturn(
                ProcessBacklog.builder()
                        .process(PICKING.getStatus())
                        .quantity(2232)
                        .build()
        );

        // WHEN
        final Monitor monitor = getMonitor.execute(input);

        // THEN
        assertEquals("Modelo de Priorización", monitor.getTitle());
        assertEquals("Estado Actual", monitor.getSubtitle1());
        assertEquals("Última actualización: Hoy - " + getCurrentTime(), monitor.getSubtitle2());

        final List<MonitorData> monitorDataList = monitor.getMonitorData();
        assertEquals(2, monitorDataList.size());

        final CurrentStatusData currentStatusData = (CurrentStatusData) monitorDataList.get(1);
        final List<Process> processes = currentStatusData.getProcesses();
        assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
        assertEquals(4, processes.size());

        final Optional<Process> optionalPicking = currentStatusData.getProcesses().stream()
                .filter(t -> PICKING.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalPicking.isPresent());
        final Process picking = optionalPicking.get();
        assertEquals(PICKING.getTitle(), picking.getTitle());
        Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertEquals(PICKING.getSubtitle(), pickingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), pickingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), pickingBacklogMetric.getType());
        assertEquals("2.232 uds.", pickingBacklogMetric.getValue());

        final Optional<Process> optionalPacking = currentStatusData.getProcesses().stream()
                .filter(t -> PACKING.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalPacking.isPresent());
        final Process packing = optionalPacking.get();
        assertEquals(PACKING.getTitle(), packing.getTitle());
        Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertEquals(PACKING.getSubtitle(), packingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), packingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), packingBacklogMetric.getType());
        assertEquals("1.442 uds.", packingBacklogMetric.getValue());

        final Optional<Process> optionalWallIn = currentStatusData.getProcesses().stream()
                .filter(t -> WALL_IN.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalWallIn.isPresent());
        final Process wallIn = optionalWallIn.get();
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertEquals(WALL_IN.getSubtitle(), wallInBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), wallInBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), wallInBacklogMetric.getType());
        assertEquals("725 uds.", wallInBacklogMetric.getValue());

        final Optional<Process> optionalOutboundPlanning = currentStatusData.getProcesses().stream()
                .filter(t -> OUTBOUND_PLANNING.getTitle().equals(t.getTitle()))
                .findFirst();
        assertTrue(optionalOutboundPlanning.isPresent());
        final Process outboundPlanning = optionalOutboundPlanning.get();
        assertEquals(OUTBOUND_PLANNING.getTitle(), outboundPlanning.getTitle());
        Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertEquals(OUTBOUND_PLANNING.getSubtitle(), planningBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), planningBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), planningBacklogMetric.getType());
        assertEquals("0 uds.", planningBacklogMetric.getValue());
        
        assertTrue(monitorDataList.get(0) instanceof DeviationData);
        DeviationData deviationData = (DeviationData) monitorDataList.get(0);
        assertEquals("-5.1%", deviationData.getMetrics().getDeviationPercentage().getValue());
        assertNull(deviationData.getMetrics().getDeviationPercentage().getStatus());
        assertEquals("arrow_down", deviationData.getMetrics().getDeviationPercentage().getIcon());
        assertEquals("905 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getCurrentUnits().getValue());
        assertEquals("1042 uds.", deviationData.getMetrics().getDeviationUnits()
                .getDetail().getForecastUnits().getValue());
        
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

        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.empty());

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