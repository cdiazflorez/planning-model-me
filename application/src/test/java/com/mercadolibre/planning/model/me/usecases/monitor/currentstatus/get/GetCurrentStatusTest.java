package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.MonitorDataType.CURRENT_STATUS;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.BATCH_SORTED;
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

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.CurrentStatusData;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentStatusTest {

    private static final List<String> STEPS = List.of("step", "area", "date_out");

    private static final List<String> GROUPERS =
            List.of(OUTBOUND_PLANNING.getStatus(), PACKING.getStatus(), PICKING.getStatus(), WALL_IN.getStatus(), PACKING_WALL.getStatus());

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

    @Mock
    private FeatureSwitches featureSwitches;

    @Mock
    private BacklogApiGateway backlogApiGateway;

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

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(false);

        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        commonMocks(input, cptFrom, cptTo, currentDate, true, false);

        // WHEN
        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        // THEN
        final Set<Process> processes = currentStatusData.getProcesses();
      assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
      assertEquals(6, processes.size());

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
    public void testExecuteOkWhenHavePutToWallFromBacklogApi() {
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

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(true);

        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        commonMocks(input, cptFrom, cptTo, currentDate, true, true);

        // WHEN
        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        // THEN
        final Set<Process> processes = currentStatusData.getProcesses();
      assertEquals(CURRENT_STATUS.getType(), currentStatusData.getType());
      assertEquals(6, processes.size());

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

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(false);

        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = false;

        commonMocks(input, cptFrom, cptTo, currentDate, false, false);

        // WHEN
        final CurrentStatusData currentStatusData = getCurrentStatus.execute(input);

        // THEN
        final Set<Process> processes = currentStatusData.getProcesses();
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

      final Process packing = processList.get(PACKING.getIndex() - 2);
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

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(false);

        final ZonedDateTime cptFrom = currentDate.truncatedTo(DAYS)
                .minusDays(7)
                .withZoneSameInstant(UTC);

        final ZonedDateTime cptTo = currentDate.truncatedTo(DAYS)
                .plusMonths(2)
                .withZoneSameInstant(UTC);

        isAnalyticsError = true;

        commonMocks(input, cptFrom, cptTo, currentDate, true, false);

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
                             final boolean hasPutToWall,
                             final boolean isEnableCallBacklogApi) {

        when(logisticCenterGateway.getConfiguration(input.getWarehouseId()))
                .thenReturn(new LogisticCenterConfiguration(getTimeZone(UTC), hasPutToWall));

        final Map<String, ProcessBacklog> mockProcessBacklog = Map.of(
            "wavingProcessBacklog", ProcessBacklog.builder().process(OUTBOUND_PLANNING.getStatus()).quantity(1442).build(),
            "packingProcessBacklog", ProcessBacklog.builder().process(PACKING.getStatus()).quantity(1442).build(),
            "pickingProcessBacklog", ProcessBacklog.builder().process(PICKING.getStatus()).quantity(725).build(),
            "immediateBacklog", ProcessBacklog.builder().process(OUTBOUND_PLANNING.getStatus()).immediateQuantity(200).build(),
            "wallInBacklog", ProcessBacklog.builder().process(WALL_IN.getStatus()).quantity(725).build(),
            "packingWallBacklog", ProcessBacklog.builder().process(PACKING_WALL.getStatus()).quantity(725).area("PW").build(),
            "batchSortedBacklog", ProcessBacklog.builder().process(BATCH_SORTED.getStatus()).quantity(725).build(),
            "batch", ProcessBacklog.builder().process(BATCH_SORTED.getStatus()).quantity(725).area("PW").build()
        );

        if (isEnableCallBacklogApi) {
          final List<Photo.Group> photoGroupList = mockProcessBacklog.values().stream()
              .flatMap(processBacklog -> {
                final List<String> steps = Arrays.asList(processBacklog.getProcess().toUpperCase(Locale.ROOT).split(","));
                return steps.stream().map(step -> new Photo.Group(
                    getRequestKeys(step, processBacklog.getArea(), cptFrom.toInstant()),
                    (int) ((processBacklog.getQuantity() / steps.size()) + 0.9),
                    0)
                );
              })
              .collect(Collectors.toList());


          when(backlogApiGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(new Photo(Instant.now(), photoGroupList));
        } else {
            when(backlogGatewayProvider.getBy(input.getWorkflow()))
                    .thenReturn(Optional.of(backlogGateway));

            when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                    OUTBOUND_PLANNING.getStatus(),
                    input.getWarehouseId(),
                    cptFrom,
                    cptTo,
                    null,
                    ORDER_GROUP_TYPE)))
                    .thenReturn(mockProcessBacklog.get("wavingProcessBacklog"));

            when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                    PACKING.getStatus(),
                    input.getWarehouseId(),
                    cptFrom,
                    cptTo,
                    null,
                    ORDER_GROUP_TYPE)))
                    .thenReturn(mockProcessBacklog.get("packingProcessBacklog"));

            when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                    PICKING.getStatus(),
                    input.getWarehouseId(),
                    cptFrom,
                    cptTo,
                    null,
                    ORDER_GROUP_TYPE)))
                    .thenReturn(mockProcessBacklog.get("pickingProcessBacklog"));

            when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput(
                    OUTBOUND_PLANNING.getStatus(),
                    input.getWarehouseId(),
                    currentDate.minusDays(1),
                    currentDate.plusDays(1),
                    null,
                    ORDER_GROUP_TYPE)))
                    .thenReturn(mockProcessBacklog.get("immediateBacklog"));

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
                  .thenReturn(mockProcessBacklog.get("wallInBacklog"));

              when(backlogGateway.getUnitBacklog(
                  new UnitProcessBacklogInput(PACKING_WALL.getStatus(), input.getWarehouseId(),
                      cptFrom, cptTo, "PW", ORDER_GROUP_TYPE)))
                  .thenReturn(mockProcessBacklog.get("packingWallBacklog"));

              when(backlogGateway.getUnitBacklog(
                  new UnitProcessBacklogInput(BATCH_SORTED.getStatus(), input.getWarehouseId(),
                      cptFrom, cptTo, null, ORDER_GROUP_TYPE)))
                  .thenReturn(mockProcessBacklog.get("batchSortedBacklog"));
            }
        }

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
            } else if (backlogMetricInput.getProcessOutbound().equals(BATCH_SORTED)) {
              return createMetric(TOTAL_BACKLOG, BATCH_SORTED, "725 uds.");
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

  private Map<BacklogGrouper, String> getRequestKeys(final String step, final String area, final Instant cpt) {
    return Map.of(
        BacklogGrouper.STEP, step,
        BacklogGrouper.AREA, area != null ? area.toUpperCase(Locale.ROOT) : "N/A",
        BacklogGrouper.DATE_OUT, cpt.toString()
    );
  }
}
