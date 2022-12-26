package com.mercadolibre.planning.model.me.usecases.monitor;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.CURRENT_STATUS;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.BATCH_SORTED;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.exception.BacklogGatewayNotSupportedException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get.GetCurrentStatus;
import com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get.GetCurrentStatusInput;
import com.mercadolibre.planning.model.me.usecases.monitor.deviation.GetDeviation;
import com.mercadolibre.planning.model.me.usecases.monitor.deviation.GetDeviationInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.Monitor;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.DeviationData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationActions;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationAppliedData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnit;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationUnitDetail;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMonitorTest {

    @InjectMocks
    private GetMonitor getMonitor;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetDeviation getDeviation;

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
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusHours(25))
                .build();

        commonMocks();

        // WHEN
        final Monitor monitor = getMonitor.execute(input);

        // THEN
        assertEquals("Modelo de Priorización", monitor.getTitle());
        assertEquals("Estado Actual", monitor.getSubtitle1());
        assertEquals("Última actualización: Hoy - " + getCurrentTime(), monitor.getSubtitle2());

        final List<MonitorData> monitorDataList = monitor.getMonitorData();
        assertEquals(2, monitorDataList.size());

        final CurrentStatusData currentStatusData = (CurrentStatusData) monitorDataList.get(1);
        final Set<Process> processes = currentStatusData.getProcesses();
      assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
      assertEquals(6, processes.size());

        List<Process> processList = new ArrayList<>(currentStatusData.getProcesses());

        final Process picking = processList.get(PICKING.getIndex());
        assertEquals(PICKING.getTitle(), picking.getTitle());
        Metric pickingBacklogMetric = picking.getMetrics().get(0);
        assertEquals(PICKING.getSubtitle(), pickingBacklogMetric.getSubtitle());
        assertEquals(TOTAL_BACKLOG.getTitle(), pickingBacklogMetric.getTitle());
        assertEquals(TOTAL_BACKLOG.getType(), pickingBacklogMetric.getType());
        assertEquals("2.232 uds.", pickingBacklogMetric.getValue());

        Metric pickingProductivityMetric = picking.getMetrics().get(1);
        assertEquals(PRODUCTIVITY.getSubtitle(), pickingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), pickingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), pickingProductivityMetric.getType());
        assertEquals("53 uds./h", pickingProductivityMetric.getValue());


        final Process packing = processList.get(PACKING.getIndex());
        assertEquals(PACKING.getTitle(), packing.getTitle());
        Metric packingBacklogMetric = packing.getMetrics().get(0);
        assertEquals(PACKING.getSubtitle(), packingBacklogMetric.getSubtitle());
        assertEquals(TOTAL_BACKLOG.getTitle(), packingBacklogMetric.getTitle());
        assertEquals(TOTAL_BACKLOG.getType(), packingBacklogMetric.getType());
        assertEquals("1.442 uds.", packingBacklogMetric.getValue());

        Metric packingProductivityMetric = packing.getMetrics().get(1);
        assertEquals(PRODUCTIVITY.getSubtitle(), packingProductivityMetric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), packingProductivityMetric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), packingProductivityMetric.getType());

        final Process packingWall = processList.get(PACKING_WALL.getIndex());
        assertEquals(PACKING_WALL.getTitle(), packingWall.getTitle());
        Metric packingWallBacklogMetric = packingWall.getMetrics().get(0);
        assertEquals(PACKING_WALL.getSubtitle(), packingWallBacklogMetric.getSubtitle());
        assertEquals(TOTAL_BACKLOG.getTitle(), packingWallBacklogMetric.getTitle());
        assertEquals(TOTAL_BACKLOG.getType(), packingWallBacklogMetric.getType());
        assertEquals("981 uds.", packingWallBacklogMetric.getValue());

        final Process wallIn = processList.get(WALL_IN.getIndex());
        assertEquals(WALL_IN.getTitle(), wallIn.getTitle());
        Metric wallInBacklogMetric = wallIn.getMetrics().get(0);
        assertEquals(WALL_IN.getSubtitle(), wallInBacklogMetric.getSubtitle());
        assertEquals(TOTAL_BACKLOG.getTitle(), wallInBacklogMetric.getTitle());
        assertEquals(TOTAL_BACKLOG.getType(), wallInBacklogMetric.getType());
        assertEquals("725 uds.", wallInBacklogMetric.getValue());

        final Process outboundPlanning = processList.get(OUTBOUND_PLANNING.getIndex());
        assertEquals(OUTBOUND_PLANNING.getTitle(), outboundPlanning.getTitle());
        Metric planningBacklogMetric = outboundPlanning.getMetrics().get(0);
        assertEquals(OUTBOUND_PLANNING.getSubtitle(), planningBacklogMetric.getSubtitle());
        assertEquals(TOTAL_BACKLOG.getTitle(), planningBacklogMetric.getTitle());
        assertEquals(TOTAL_BACKLOG.getType(), planningBacklogMetric.getType());
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

    private void commonMocks() {
        when(getCurrentStatus.execute(any(GetCurrentStatusInput.class)))
                .thenReturn(CurrentStatusData.builder().processes(
                        new TreeSet<>(List.of(
                                Process.builder().title(OUTBOUND_PLANNING.getTitle())
                                        .metrics(
                                                List.of(createMetric(TOTAL_BACKLOG.getTitle(),
                                                        TOTAL_BACKLOG.getType(),
                                                        OUTBOUND_PLANNING.getSubtitle(),
                                                        "0 uds.")
                                                )
                                        ).build(),
                                Process.builder().title(PICKING.getTitle())
                                        .metrics(
                                                List.of(
                                                        createMetric(TOTAL_BACKLOG.getTitle(),
                                                                TOTAL_BACKLOG.getType(),
                                                                PICKING.getSubtitle(),
                                                                "2.232 uds."),
                                                        createMetric(PRODUCTIVITY.getTitle(),
                                                                PRODUCTIVITY.getType(),
                                                                PRODUCTIVITY.getSubtitle(),
                                                                "53 uds./h")
                                                )
                                        ).build(),
                                Process.builder().title(PACKING.getTitle())
                                        .metrics(
                                                List.of(
                                                        createMetric(TOTAL_BACKLOG.getTitle(),
                                                                TOTAL_BACKLOG.getType(),
                                                                PACKING.getSubtitle(),
                                                                "1.442 uds."),
                                                        createMetric(PRODUCTIVITY.getTitle(),
                                                                PRODUCTIVITY.getType(),
                                                                PRODUCTIVITY.getSubtitle(),
                                                                "53 uds./h")
                                                )
                                        ).build(),
                                Process.builder().title(PACKING_WALL.getTitle())
                                        .metrics(
                                                List.of(createMetric(TOTAL_BACKLOG.getTitle(),
                                                        TOTAL_BACKLOG.getType(),
                                                        PACKING_WALL.getSubtitle(),
                                                    "981 uds.")
                                                )
                                        ).build(),
                            Process.builder().title(WALL_IN.getTitle())
                                .metrics(
                                    List.of(createMetric(TOTAL_BACKLOG.getTitle(),
                                        TOTAL_BACKLOG.getType(),
                                        WALL_IN.getSubtitle(),
                                        "725 uds.")
                                    )
                                ).build(),
                            Process.builder().title(BATCH_SORTED.getTitle())
                                .metrics(
                                    List.of(createMetric(TOTAL_BACKLOG.getTitle(),
                                        TOTAL_BACKLOG.getType(),
                                        BATCH_SORTED.getSubtitle(),
                                        "725 uds.")
                                    )
                                ).build()
                        ))
                ).build());

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(getDeviation.execute(any(GetDeviationInput.class))
        ).thenReturn(mockDeviation());
    }

    private DeviationData mockDeviation() {
        return new DeviationData(DeviationMetric.builder()
                .deviationPercentage(Metric.builder()
                        .title("% Desviación FCST / Ventas")
                        .value("-13.15%")
                        .status(null)
                        .icon("arrow_down")
                        .build())
                .deviationUnits(DeviationUnit.builder()
                        .title("Desviación en unidades")
                        .value("137 uds.")
                        .detail(DeviationUnitDetail.builder()
                                .forecastUnits(Metric.builder()
                                        .title("Cantidad Forecast")
                                        .value("1042 uds.")
                                        .build())
                                .currentUnits(Metric.builder()
                                        .title("Cantidad Real")
                                        .value("905 uds.")
                                        .build())
                                .build())
                        .build())
                .build(),
                DeviationActions.builder()
                        .applyLabel("Ajustar forecast")
                        .unapplyLabel("Volver al forecast")
                        .appliedData(DeviationAppliedData.builder()
                                .title("Se ajustó el forecast 5.80%s de 02:30 a 12:30")
                                .icon("info")
                                .build())
                .build());
    }

    private Metric createMetric(final String title, final String type, final String subtitle,
                                final String value) {
        return Metric.builder().title(title)
                .type(type)
                .subtitle(subtitle)
                .value(value)
                .build();
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

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(getCurrentStatus.execute(any(GetCurrentStatusInput.class)))
                .thenThrow(BacklogGatewayNotSupportedException.class);

        // WHEN - THEN
        assertThrows(BacklogGatewayNotSupportedException.class, () -> getMonitor.execute(input));
    }

    private String getCurrentTime() {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime currentDate = convertToTimeZone(TIME_ZONE.toZoneId(), now);
        return currentDate.format(HOUR_MINUTES_FORMATTER);
    }
}
