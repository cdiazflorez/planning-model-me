package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.ProcessDetail;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.entities.monitor.VariablesPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.WorkflowBacklogDetail;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorInputDto;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import com.mercadolibre.planning.model.me.utils.TestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBacklogMonitorTest {
    private static final List<ZonedDateTime> DATES = of(
            parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME)
    );

    private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOW = Map.of(
            FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING),
            FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
    );

    private static final ZonedDateTime DATE_CURRENT = DATES.get(1);

    private static final ZonedDateTime DATE_FROM = DATES.get(0);

    private static final ZonedDateTime DATE_TO = DATES.get(3);

    @InjectMocks
    private GetBacklogMonitor getBacklogMonitor;

    @Mock
    private BacklogApiAdapter backlogApiAdapter;

    @Mock
    private GetProcessThroughput getProcessThroughput;

    @Mock
    private GetHistoricalBacklog getHistoricalBacklog;

    @Mock
    private GetBacklogLimits getBacklogLimits;

    private MockedStatic<DateUtils> mockDt;

    @BeforeEach
    void setUp() {
        mockDt = mockStatic(DateUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockDt.close();
    }

    @Test
    void testExecuteOK() {
        // GIVEN
        mockDateUtils(mockDt);
        mockBacklogApiResponse(input(FBM_WMS_OUTBOUND));
        mockProjectedBacklog(input(FBM_WMS_OUTBOUND));
        mockHistoricalBacklog(input(FBM_WMS_OUTBOUND));
        mockThroughput(input(FBM_WMS_OUTBOUND));
        mockBacklogLimits(input(FBM_WMS_OUTBOUND));

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input(FBM_WMS_OUTBOUND));

        // THEN
        assertNotNull(orders);
        assertEquals("fbm-wms-outbound", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertWavingBacklogResults(waving);
        assertEquals(200, waving.getBacklogs().get(0).getHistorical().getUnits());
        assertEquals(20, waving.getBacklogs().get(0).getHistorical().getMinutes());

        assertEquals(80, waving.getBacklogs().get(3).getHistorical().getUnits());
        assertEquals(8, waving.getBacklogs().get(3).getHistorical().getMinutes());

    }

    @Test
    void testGetCurrentBacklogError() {
        // GIVEN
        final GetBacklogMonitorInputDto input = input(FBM_WMS_OUTBOUND);

        when(backlogApiAdapter.getCurrentBacklog(
                input.getRequestDate(),
                input.getWarehouseId(),
                of(input.getWorkflow()),
                PROCESS_BY_WORKFLOW.get(input.getWorkflow()),
                of(PROCESS),
                input.getDateFrom(),
                input.getRequestDate().truncatedTo(ChronoUnit.SECONDS),
                input.getRequestDate(),
                input.getRequestDate().plus(24, ChronoUnit.HOURS))
        ).thenThrow(new TestException());

        // WHEN
        assertThrows(
                TestException.class,
                () -> getBacklogMonitor.execute(input)
        );
    }

    @ParameterizedTest
    @MethodSource("mockParameterizedConfiguration")
    void testGetProjectedBacklogError(final Workflow workflow) {
        // GIVEN
        mockDateUtils(mockDt);
        mockBacklogApiResponse(input(workflow));
        mockHistoricalBacklog(input(workflow));
        mockThroughput(input(workflow));

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input(workflow));

        // THEN
        assertNotNull(orders);
        assertEquals(workflow.getName(), orders.getWorkflow());

        final ProcessDetail firstProcess = orders.getProcesses().get(0);
        assertEquals(2, firstProcess.getBacklogs().size());
    }

    @Test
    void testGetHistoricalBacklogError() {
        // GIVEN
        mockDateUtils(mockDt);
        mockBacklogApiResponse(input(FBM_WMS_OUTBOUND));
        mockProjectedBacklog(input(FBM_WMS_OUTBOUND));
        mockThroughput(input(FBM_WMS_OUTBOUND));
        mockBacklogLimits(input(FBM_WMS_OUTBOUND));

        final var request = new GetHistoricalBacklogInput(
                DATE_CURRENT.toInstant(),
                WAREHOUSE_ID,
                of(FBM_WMS_OUTBOUND),
                of(WAVING, PICKING, PACKING),
                DATE_FROM.minusWeeks(2L).toInstant(),
                DATE_FROM.toInstant()
        );

        when(getHistoricalBacklog.execute(request))
                .thenThrow(TestException.class);

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input(FBM_WMS_OUTBOUND));

        // THEN
        assertNotNull(orders);
        assertEquals("fbm-wms-outbound", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertWavingBacklogResults(waving);
        assertNull(waving.getBacklogs().get(0).getHistorical().getUnits());
        assertNull(waving.getBacklogs().get(3).getHistorical().getUnits());
        assertNull(waving.getBacklogs().get(0).getHistorical().getMinutes());
        assertNull(waving.getBacklogs().get(3).getHistorical().getMinutes());
    }

    @Test
    void testGetThroughputError() {
        // GIVEN
        mockDateUtils(mockDt);
        mockBacklogApiResponse(input(FBM_WMS_OUTBOUND));
        mockProjectedBacklog(input(FBM_WMS_OUTBOUND));
        mockHistoricalBacklog(input(FBM_WMS_OUTBOUND));
        mockBacklogLimits(input(FBM_WMS_OUTBOUND));

        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(List.of(WAVING, PICKING, PACKING))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();

        when(getProcessThroughput.execute(request))
                .thenThrow(TestException.class);

        // WHEN
        final WorkflowBacklogDetail orders = getBacklogMonitor.execute(input(FBM_WMS_OUTBOUND));

        // THEN
        assertNotNull(orders);
        assertEquals("fbm-wms-outbound", orders.getWorkflow());

        // waving
        final ProcessDetail waving = orders.getProcesses().get(0);

        assertEquals("waving", waving.getProcess());
        assertEquals(4, waving.getBacklogs().size());
        assertEquals(150, waving.getTotal().getUnits());
        assertEquals(0, waving.getTotal().getMinutes());
    }

    private void assertWavingBacklogResults(final ProcessDetail waving) {
        // waving
        assertEquals("waving", waving.getProcess());
        assertEquals(150, waving.getTotal().getUnits());
        assertEquals(73, waving.getTotal().getMinutes());
        assertEquals(4, waving.getBacklogs().size());

        // past backlog
        final VariablesPhoto wavingPastBacklog = waving.getBacklogs().get(0);
        assertEquals(DATE_FROM.toInstant(), wavingPastBacklog.getDate());
        assertEquals(100, wavingPastBacklog.getCurrent().getUnits());
        assertEquals(127, wavingPastBacklog.getCurrent().getMinutes());

        final VariablesPhoto wavingNullMinutesBacklog = waving.getBacklogs().get(2);
        assertEquals(DATES.get(2).toInstant(), wavingNullMinutesBacklog.getDate());
        //assertNull(wavingNullMinutesBacklog.getCurrent().getMinutes());

        // projected backlog
        final VariablesPhoto wavingProjectedBacklog = waving.getBacklogs().get(3);
        assertEquals(DATES.get(3).toInstant(), wavingProjectedBacklog.getDate());
        assertEquals(250, wavingProjectedBacklog.getCurrent().getUnits());
        assertEquals(60, wavingProjectedBacklog.getCurrent().getMinutes());
    }

    private GetBacklogMonitorInputDto input(final Workflow workflow) {
        return new GetBacklogMonitorInputDto(
                DATE_CURRENT.toInstant(),
                WAREHOUSE_ID,
                workflow,
                DATE_FROM.toInstant(),
                DATE_TO.toInstant(),
                0L
        );
    }

    private void mockBacklogApiResponse(final GetBacklogMonitorInputDto input) {
        Instant firstDate = DATES.get(0).toInstant();
        Instant secondDate = DATES.get(1).toInstant();

        final boolean isOutbound = input.getWorkflow() == FBM_WMS_OUTBOUND;

        when(backlogApiAdapter.getCurrentBacklog(input.getRequestDate(),
                input.getWarehouseId(),
                of(input.getWorkflow()),
                PROCESS_BY_WORKFLOW.get(input.getWorkflow()),
                of(PROCESS),
                input.getDateFrom(),
                input.getRequestDate().truncatedTo(ChronoUnit.SECONDS),
                isOutbound ? input.getRequestDate() : input.getRequestDate().minus(168, ChronoUnit.HOURS),
                isOutbound
                        ? input.getRequestDate().plus(24, ChronoUnit.HOURS)
                        : input.getRequestDate().plus(168, ChronoUnit.HOURS)
                )
        ).thenReturn(isOutbound ? of(
                new Consolidation(firstDate, Map.of("process", "waving"), 100),
                new Consolidation(secondDate, Map.of("process", "waving"), 150),
                new Consolidation(firstDate, Map.of("process", "picking"), 300),
                new Consolidation(secondDate, Map.of("process", "picking"), 350),
                new Consolidation(firstDate, Map.of("process", "packing"), 6000),
                new Consolidation(secondDate, Map.of("process", "packing"), 8000)
        ) : of(
                new Consolidation(firstDate, Map.of("process", "check_in"), 100),
                new Consolidation(secondDate, Map.of("process", "check_in"), 150),
                new Consolidation(firstDate, Map.of("process", "put_away"), 300),
                new Consolidation(secondDate, Map.of("process", "put_away"), 350)));
    }

    private void mockProjectedBacklog(final GetBacklogMonitorInputDto input) {

        final Instant currentFirstDate = DATES.get(0).toInstant();
        final Instant currentSecondDate = DATES.get(1).toInstant();
        final ZonedDateTime firstDate = DATES.get(2);
        final ZonedDateTime secondDate = DATES.get(3);

        final boolean isOutbound = input.getWorkflow() == FBM_WMS_OUTBOUND;

        when(backlogApiAdapter.getProjectedBacklog(
                input.getWarehouseId(),
                input.getWorkflow(),
                PROCESS_BY_WORKFLOW.get(input.getWorkflow()),
                input.getRequestDate().atZone(ZoneId.of("UTC")).withFixedOffsetZone(),
                input.getRequestDate().atZone(ZoneId.of("UTC")).withFixedOffsetZone().plusHours(24),
                input.getCallerId(), isOutbound ? of(
                        new Consolidation(currentFirstDate, Map.of("process", "waving"), 100),
                        new Consolidation(currentSecondDate, Map.of("process", "waving"), 150),
                        new Consolidation(currentFirstDate, Map.of("process", "picking"), 300),
                        new Consolidation(currentSecondDate, Map.of("process", "picking"), 350),
                        new Consolidation(currentFirstDate, Map.of("process", "packing"), 6000),
                        new Consolidation(currentSecondDate, Map.of("process", "packing"), 8000))
                        : of(
                        new Consolidation(currentFirstDate, Map.of("process", "check_in"), 100),
                        new Consolidation(currentSecondDate, Map.of("process", "check_in"), 150),
                        new Consolidation(currentFirstDate, Map.of("process", "put_away"), 300),
                        new Consolidation(currentSecondDate, Map.of("process", "put_away"), 350))))
                .thenReturn(isOutbound ? of(
                        new BacklogProjectionResponse(WAVING, of(new ProjectionValue(firstDate, 125),
                                new ProjectionValue(secondDate, 250))),
                        new BacklogProjectionResponse(PICKING, of(new ProjectionValue(firstDate, 410),
                                new ProjectionValue(secondDate, 630))),
                        new BacklogProjectionResponse(PACKING, of(new ProjectionValue(firstDate, 888),
                                new ProjectionValue(secondDate, 999))))
                        : of(
                        new BacklogProjectionResponse(CHECK_IN, of(new ProjectionValue(firstDate, 125),
                                new ProjectionValue(secondDate, 250))),
                        new BacklogProjectionResponse(PUT_AWAY, of(new ProjectionValue(firstDate, 410),
                                new ProjectionValue(secondDate, 630)))));
    }

    private void mockHistoricalBacklog(final GetBacklogMonitorInputDto input) {
        final var firstDateHash = 5820;
        final var secondDateHash = 5880;
        final var thirdDateHash = 5940;
        final var fourthDateHash = 6000;

        final var request = new GetHistoricalBacklogInput(
                input.getRequestDate(),
                input.getWarehouseId(),
                of(input.getWorkflow()),
                PROCESS_BY_WORKFLOW.get(input.getWorkflow()),
                input.getDateFrom(),
                input.getDateTo()
        );

        final boolean isOutbound = input.getWorkflow() == FBM_WMS_OUTBOUND;

        when(getHistoricalBacklog.execute(request))
                .thenReturn(isOutbound
                        ? Map.of(
                                WAVING, new HistoricalBacklog(
                                        Map.of(
                                                firstDateHash, new UnitMeasure(200, 20),
                                                secondDateHash, new UnitMeasure(100, 10),
                                                thirdDateHash, new UnitMeasure(50, 5),
                                                fourthDateHash, new UnitMeasure(80, 8))),
                                PICKING, new HistoricalBacklog(
                                        Map.of(
                                                firstDateHash, new UnitMeasure(22, 2),
                                                secondDateHash, new UnitMeasure(111, 11),
                                                thirdDateHash, new UnitMeasure(150, 15),
                                                fourthDateHash, new UnitMeasure(215, 21))),
                                PACKING, new HistoricalBacklog(
                                        Map.of(
                                                firstDateHash, new UnitMeasure(0, 0),
                                                secondDateHash, new UnitMeasure(120, 12),
                                                thirdDateHash, new UnitMeasure(220, 22),
                                                fourthDateHash, new UnitMeasure(420, 42))))
                        : Map.of(
                        CHECK_IN, new HistoricalBacklog(
                                Map.of(
                                        firstDateHash, new UnitMeasure(200, 20),
                                        secondDateHash, new UnitMeasure(100, 10),
                                        thirdDateHash, new UnitMeasure(50, 5),
                                        fourthDateHash, new UnitMeasure(80, 8))),
                        PUT_AWAY, new HistoricalBacklog(
                                Map.of(
                                        firstDateHash, new UnitMeasure(22, 2),
                                        secondDateHash, new UnitMeasure(111, 11),
                                        thirdDateHash, new UnitMeasure(150, 15),
                                        fourthDateHash, new UnitMeasure(215, 21)))
                ));
    }

    private void mockThroughput(final GetBacklogMonitorInputDto input) {
        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processes(PROCESS_BY_WORKFLOW.get(input.getWorkflow()))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO.plusHours(24))
                .build();

        final boolean isOutbound = input.getWorkflow() == FBM_WMS_OUTBOUND;

        when(getProcessThroughput.execute(request))
                .thenReturn(new GetThroughputResult(isOutbound
                        ? Map.of(
                                WAVING, Map.of(
                                        DATES.get(0), 10,
                                        DATES.get(1), 15,
                                        DATES.get(2), 600,
                                        DATES.get(3), 20),
                                PICKING, Map.of(
                                        DATES.get(0), 3,
                                        DATES.get(1), 1,
                                        DATES.get(2), 4,
                                        DATES.get(3), 5),
                                PACKING, Map.of(
                                        DATES.get(0), 1000,
                                        DATES.get(1), 50,
                                        DATES.get(2), 20,
                                        DATES.get(3), 300))
                        : Map.of(
                        CHECK_IN, Map.of(
                                DATES.get(0), 10,
                                DATES.get(1), 15,
                                DATES.get(2), 0,
                                DATES.get(3), 20),
                        PUT_AWAY, Map.of(
                                DATES.get(0), 3,
                                DATES.get(1), 1,
                                DATES.get(2), 4,
                                DATES.get(3), 5))));
    }

    private void mockBacklogLimits(final GetBacklogMonitorInputDto input) {
        final var request = GetBacklogLimitsInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processes(PROCESS_BY_WORKFLOW.get(input.getWorkflow()))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build();

        final boolean isOutbound = input.getWorkflow() == FBM_WMS_OUTBOUND;

        when(getBacklogLimits.execute(request)).thenReturn(isOutbound
                ? Map.of(
                        WAVING, Map.of(
                                DATES.get(0).toInstant(), new BacklogLimit(5, 15),
                                DATES.get(1).toInstant(), new BacklogLimit(7, 21),
                                DATES.get(2).toInstant(), new BacklogLimit(3, 21),
                                DATES.get(3).toInstant(), new BacklogLimit(0, -1)),
                        PICKING, Map.of(
                                DATES.get(0).toInstant(), new BacklogLimit(-1, -1),
                                DATES.get(1).toInstant(), new BacklogLimit(-1, -1),
                                DATES.get(2).toInstant(), new BacklogLimit(-1, -1),
                                DATES.get(3).toInstant(), new BacklogLimit(-1, -1)),
                        PACKING, Map.of(
                                DATES.get(0).toInstant(), new BacklogLimit(0, 20),
                                DATES.get(1).toInstant(), new BacklogLimit(0, 15),
                                DATES.get(2).toInstant(), new BacklogLimit(0, 10),
                                DATES.get(3).toInstant(), new BacklogLimit(0, 10)))
                : Map.of(
                CHECK_IN, Map.of(
                        DATES.get(0).toInstant(), new BacklogLimit(5, 15),
                        DATES.get(1).toInstant(), new BacklogLimit(7, 21),
                        DATES.get(2).toInstant(), new BacklogLimit(3, 21),
                        DATES.get(3).toInstant(), new BacklogLimit(0, -1)),
                PUT_AWAY, Map.of(
                        DATES.get(0).toInstant(), new BacklogLimit(-1, -1),
                        DATES.get(1).toInstant(), new BacklogLimit(-1, -1),
                        DATES.get(2).toInstant(), new BacklogLimit(-1, -1),
                        DATES.get(3).toInstant(), new BacklogLimit(-1, -1))
        ));
    }

    private void mockDateUtils(MockedStatic<DateUtils> mockDt) {
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(0).toInstant())).thenReturn(5820);
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(1).toInstant())).thenReturn(5880);
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(2).toInstant())).thenReturn(5940);
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(3).toInstant())).thenReturn(6000);
    }

    public static Stream<Arguments> mockParameterizedConfiguration() {
        return Stream.of(
                Arguments.of(FBM_WMS_OUTBOUND),
                Arguments.of(FBM_WMS_INBOUND)
        );
    }
}
