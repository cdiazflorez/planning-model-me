package com.mercadolibre.planning.model.me.usecases.currentstatus;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorData;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Process;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.StatusType.PENDING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.StatusType.TO_GROUP;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.StatusType.TO_PACK;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.StatusType.TO_PICK;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.MetricType.BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMonitorTest {

    @InjectMocks
    private GetMonitor getMonitor;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private BacklogGateway backlogGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    private static final TimeZone TIME_ZONE = getDefault();

    @Test
    public void testExecuteOk() {
        // GIVEN
        final GetMonitorInput input = GetMonitorInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .build();

        final String status = "status";
        List<Map<String, String>> statuses = List.of(
                Map.of(status, PENDING.toName()),
                Map.of(status, TO_PICK.toName()),
                Map.of(status, TO_PACK.toName()),
                Map.of(status, TO_GROUP.toName())
        );


        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.of(backlogGateway));
        when(backlogGateway.getBacklog(statuses,
                input.getWarehouseId(),
                input.getDateFrom(),
                input.getDateTo()
        )).thenReturn(List.of(
                ProcessBacklog.builder()
                        .process(PICKING.getStatus())
                        .quantity(2232)
                        .build(),
                ProcessBacklog.builder()
                        .process(PACKING.getStatus())
                        .quantity(1442)
                        .build(),
                ProcessBacklog.builder()
                        .process(WALL_IN.getStatus())
                        .quantity(725)
                        .build()
        ));

        // WHEN
        final Monitor monitor = getMonitor.execute(input);

        // THEN
        final List<MonitorData> monitorDataList = monitor.getMonitorData();
        assertEquals(2, monitorDataList.size());

        final CurrentStatusData currentStatusData = (CurrentStatusData) monitorDataList.get(1);
        final List<Process> processes = currentStatusData.getProcesses();
        assertEquals(4, processes.size());

        final Process picking = currentStatusData.getProcesses().get(0);
        assertEquals(PICKING.getTitle(), picking.getTitle());
        Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertEquals(PICKING.getSubtitle(), pickingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), pickingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), pickingBacklogMetric.getType());
        assertEquals("2.232 uds.", pickingBacklogMetric.getValue());

        final Process packing = currentStatusData.getProcesses().get(1);
        assertEquals(PACKING.getTitle(), packing.getTitle());
        Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertEquals(PACKING.getSubtitle(), packingBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), packingBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), packingBacklogMetric.getType());
        assertEquals("1.442 uds.", packingBacklogMetric.getValue());

        final Process wallIn = currentStatusData.getProcesses().get(2);
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertEquals(WALL_IN.getSubtitle(), wallInBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), wallInBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), wallInBacklogMetric.getType());
        assertEquals("725 uds.", wallInBacklogMetric.getValue());

        final Process outboundPlanning = currentStatusData.getProcesses().get(3);
        assertEquals(OUTBOUND_PLANNING.getTitle(), outboundPlanning.getTitle());
        Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertEquals(OUTBOUND_PLANNING.getSubtitle(), planningBacklogMetric.getSubtitle());
        assertEquals(BACKLOG.getTitle(), planningBacklogMetric.getTitle());
        assertEquals(BACKLOG.getType(), planningBacklogMetric.getType());
        assertEquals("0 uds.", planningBacklogMetric.getValue());
    }
}