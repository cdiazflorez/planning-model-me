package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Process;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.GetBacklogMetricUseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.immediatebacklog.get.GetImmediateBacklogMetricUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.CURRENT_STATUS;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.WALL_IN;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.ORDER_GROUP_TYPE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
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
    private BacklogGateway backlogGateway;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private GetBacklogMetricUseCase getBacklogMetric;

    @Mock
    private GetImmediateBacklogMetricUseCase getImmediateBacklogMetricUseCase;

    private boolean isAnalyticsError;

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

        commonMocks(input, cptFrom, cptTo, currentDate, true);

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
        assertMetric(planningBacklogMetric, OUTBOUND_PLANNING.getSubtitle(),
                TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "0 uds.");

        final Metric planningImmediateBacklogMetric = outboundPlanning.getMetrics().get(1);
        assertMetric(planningImmediateBacklogMetric, null,
                IMMEDIATE_BACKLOG.getTitle(), IMMEDIATE_BACKLOG.getType(), "10 uds.");

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

        commonMocks(input, cptFrom, cptTo, currentDate,false);

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
        assertMetric(planningBacklogMetric, OUTBOUND_PLANNING.getSubtitle(),
                TOTAL_BACKLOG.getTitle(),
                TOTAL_BACKLOG.getType(), "0 uds.");

        final Metric planningImmediateBacklogMetric = outboundPlanning.getMetrics().get(1);
        assertMetric(planningImmediateBacklogMetric, null,
                IMMEDIATE_BACKLOG.getTitle(), IMMEDIATE_BACKLOG.getType(), "10 uds.");

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

        final ProcessBacklog wavingProcessBacklog = ProcessBacklog.builder()
                .process(OUTBOUND_PLANNING.getStatus())
                .quantity(1442)
                .build();

        when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                OUTBOUND_PLANNING.getStatus(),
                input.getWarehouseId(),
                cptFrom,
                cptTo,
                null,
                ORDER_GROUP_TYPE)))
                .thenReturn(wavingProcessBacklog);

        final ProcessBacklog packingProcessBacklog = ProcessBacklog.builder()
                .process(PACKING.getStatus())
                .quantity(1442)
                .build();

        when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                PACKING.getStatus(),
                input.getWarehouseId(),
                cptFrom,
                cptTo,
                null,
                ORDER_GROUP_TYPE)))
                .thenReturn(packingProcessBacklog);

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
                ORDER_GROUP_TYPE)))
                .thenReturn(pickingProcessBacklog);

        when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                OUTBOUND_PLANNING.getStatus(),
                input.getWarehouseId(),
                currentDate.minusDays(1),
                currentDate.plusDays(1),
                null,
                ORDER_GROUP_TYPE)))
                .thenReturn(ProcessBacklog.builder()
                        .process(OUTBOUND_PLANNING.getStatus())
                        .immediateQuantity(200)
                        .build());

        if (hasPutToWall) {
            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, null, ORDER_GROUP_TYPE)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(WALL_IN.getStatus())
                            .quantity(725)
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(PACKING.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, "PW", ORDER_GROUP_TYPE)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(PACKING.getStatus())
                            .quantity(725)
                            .area("PW")
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(WALL_IN.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, null, ORDER_GROUP_TYPE)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(WALL_IN.getStatus())
                            .quantity(725)
                            .build());

            when(backlogGateway.getUnitBacklog(
                    new UnitProcessBacklogInput(PACKING_WALL.getStatus(), input.getWarehouseId(),
                            cptFrom, cptTo, "PW", ORDER_GROUP_TYPE)))
                    .thenReturn(ProcessBacklog.builder()
                            .process(PACKING.getStatus())
                            .quantity(725)
                            .area("PW")
                            .build());
        }

        when(logisticCenterGateway.getConfiguration(input.getWarehouseId()))
                .thenReturn(new LogisticCenterConfiguration(getTimeZone(UTC), hasPutToWall));

        mockGetBacklogMetric();
        mockGetImmediateBacklogMetric();
    }

    private void assertMetric(final Metric metric, final String expectedSubtitle,
                              final String expectedTitle, final String expectedType,
                              final String expectedValue) {
        assertEquals(expectedSubtitle, metric.getSubtitle());
        assertEquals(expectedTitle, metric.getTitle());
        assertEquals(expectedType, metric.getType());
        assertEquals(expectedValue, metric.getValue());
    }

    private void mockGetBacklogMetric() {
        when(getBacklogMetric.execute(any(BacklogMetricInput.class))).thenAnswer(invocation -> {
            BacklogMetricInput backlogMetricInput = invocation.getArgument(0);
            if (backlogMetricInput.getProcessOutbound().equals(OUTBOUND_PLANNING)) {
                return createMetric(TOTAL_BACKLOG, OUTBOUND_PLANNING, "0 uds.");
            } else if (backlogMetricInput.getProcessOutbound().equals(PICKING)) {
                return createMetric(TOTAL_BACKLOG, PICKING, "10 uds.");
            } else if (backlogMetricInput.getProcessOutbound().equals(PACKING_WALL)) {
                return createMetric(TOTAL_BACKLOG, PACKING_WALL, "33 uds.");
            } else if (backlogMetricInput.getProcessOutbound().equals(PACKING)) {
                return createMetric(TOTAL_BACKLOG, PACKING, "725 uds.");
            } else if (backlogMetricInput.getProcessOutbound().equals(WALL_IN)) {
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

    private Metric createMetric(final MetricType metricType,
                                final ProcessOutbound processOutbound,
                                final String value) {

        final String subtitle = metricType.equals(TOTAL_BACKLOG)
                ? processOutbound.getSubtitle()
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
