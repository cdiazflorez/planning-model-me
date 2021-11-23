package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagVarPhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBacklogMonitorDetailsTest {
    private static final List<ZonedDateTime> DATES = of(
            parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
            parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME)
    );

    private static final ZonedDateTime DATE_CURRENT = DATES.get(1);

    private static final ZonedDateTime DATE_FROM = DATES.get(0);

    private static final ZonedDateTime DATE_TO = DATES.get(3);

    @InjectMocks
    private GetBacklogMonitorDetails getBacklogMonitor;

    @Mock
    private BacklogApiGateway backlogApiGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private ProjectBacklog backlogProjection;

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
    void testGetBacklogDetailsWithAreas() {
        // GIVEN
        mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));
        mockDateUtils(mockDt);

        mockPastBacklogWithAreas();
        mockProjectedBacklog();
        mockThroughput(PICKING);
        mockHistoricalBacklog(PICKING);
        mockBacklogLimits(PICKING);

        GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
                DATE_CURRENT.toInstant(),
                WAREHOUSE_ID,
                "outbound-orders",
                PICKING,
                DATE_FROM.toInstant(),
                DATE_TO.toInstant(),
                999L
        );

        // WHEN
        var response = getBacklogMonitor.execute(input);

        // THEN
        var results = response.getDates();
        assertEquals(4, results.size());

        var firstResult = results.get(0);
        assertEquals(DATE_FROM.toInstant(), firstResult.getDate());

        var firstAreas = firstResult.getAreas();
        assertEquals(2, firstAreas.size());
        assertEquals("RK-H", firstAreas.get(0).getId());
        assertEquals(15, firstAreas.get(0).getValue().getUnits());
        assertEquals(90, firstAreas.get(0).getValue().getMinutes());

        assertEquals(2, results.get(1).getAreas().size());

        var lastResults = results.get(3);
        assertEquals(DATE_TO.toInstant(), lastResults.getDate());
        assertNotNull(lastResults.getAreas());

        var graph = response.getProcess();
        assertEquals("picking", graph.getProcess());
        assertEquals(50, graph.getTotal().getUnits());
        assertEquals(120, graph.getTotal().getMinutes());
        assertEquals(4, graph.getBacklogs().size());

        var graphFirstResult = graph.getBacklogs().get(0);
        assertNotNull(graphFirstResult.getHistorical());
        assertEquals(22, graphFirstResult.getHistorical().getUnits());
        assertEquals(2, graphFirstResult.getHistorical().getMinutes());
    }

    @Test
    void testGetBacklogDetailsWithoutTargets() {
        // GIVEN
        mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));
        mockPastBacklogWithAreas();
        mockProjectedBacklog();
        mockThroughput(PICKING);
        mockHistoricalBacklog(PICKING);
        mockBacklogLimits(PICKING);

        // WHEN
        GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
                DATE_CURRENT.toInstant(),
                WAREHOUSE_ID,
                "outbound-orders",
                PICKING,
                DATE_FROM.toInstant(),
                DATE_TO.toInstant(),
                999L
        );

        // WHEN
        var response = getBacklogMonitor.execute(input);

        // THEN
        var results = response.getDates();
        assertEquals(4, results.size());

        var firstResult = results.get(0);
        assertEquals(DATE_FROM.toInstant(), firstResult.getDate());
        assertNull(firstResult.getTarget());

        var firstTotals = firstResult.getTotal();
        assertEquals(90, firstTotals.getUnits());
        assertEquals(540, firstTotals.getMinutes());

        var firstAreas = firstResult.getAreas();
        assertEquals(2, firstAreas.size());
        assertEquals("RK-H", firstAreas.get(0).getId());
        assertEquals(15, firstAreas.get(0).getValue().getUnits());
        assertEquals(90, firstAreas.get(0).getValue().getMinutes());

        var lastResults = results.get(3);
        assertEquals(DATE_TO.toInstant(), lastResults.getDate());
        assertNotNull(lastResults.getAreas());
        assertNull(lastResults.getTarget());
        assertEquals(500, lastResults.getTotal().getUnits());
        assertEquals(6000, lastResults.getTotal().getMinutes());

        verify(planningModelGateway, never()).getPerformedProcessing(any());
    }

    @Test
    void testGetBacklogDetailsWithoutAreas() {
        // GIVEN
        mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));
        mockPastBacklogWithoutAreas();
        mockProjectedBacklog();
        mockTargetBacklog();
        mockThroughput(WAVING);
        mockHistoricalBacklog(WAVING);
        mockBacklogLimits(WAVING);

        // WHEN
        GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
                DATE_CURRENT.toInstant(),
                WAREHOUSE_ID,
                "outbound-orders",
                WAVING,
                DATE_FROM.toInstant(),
                DATE_TO.toInstant(),
                999L
        );

        // WHEN
        var response = getBacklogMonitor.execute(input);

        // THEN
        var results = response.getDates();
        assertEquals(4, results.size());

        var firstResult = results.get(0);
        assertEquals(DATE_FROM.toInstant(), firstResult.getDate());
        assertNull(firstResult.getAreas());

        var firstTotals = firstResult.getTotal();
        assertEquals(28, firstTotals.getUnits());
        assertEquals(168, firstTotals.getMinutes());

        var firstTargets = firstResult.getTarget();
        assertEquals(10, firstTargets.getUnits());
        assertEquals(60, firstTargets.getMinutes());

        var lastResults = results.get(3);
        assertEquals(DATE_TO.toInstant(), lastResults.getDate());
        assertNull(lastResults.getAreas());

        assertEquals(60, lastResults.getTarget().getUnits());
        assertEquals(720, lastResults.getTarget().getMinutes());

        assertEquals(500, lastResults.getTotal().getUnits());
        assertEquals(6000, lastResults.getTotal().getMinutes());

        var graph = response.getProcess();
        assertEquals("waving", graph.getProcess());
        assertEquals(50, graph.getTotal().getUnits());
        assertEquals(120, graph.getTotal().getMinutes());
        assertEquals(4, graph.getBacklogs().size());
    }

    private void mockPastBacklogWithAreas() {
        Instant firstDate = DATES.get(0).toInstant();
        Instant secondDate = DATES.get(1).toInstant();

        var rkH = Map.of("area", "RK-H");
        var rkL = Map.of("area", "RK-L");

        when(backlogApiGateway.getBacklog(any()))
                .thenReturn(
                        List.of(
                                new Consolidation(firstDate, rkH, 15),
                                new Consolidation(secondDate, rkH, 50),
                                new Consolidation(firstDate, rkL, 75)
                        ));
    }

    private void mockPastBacklogWithoutAreas() {
        var na = Map.of("area", "N/A");
        when(backlogApiGateway.getBacklog(any()))
                .thenReturn(
                        List.of(
                                new Consolidation(DATES.get(0).toInstant(), na, 28),
                                new Consolidation(DATES.get(1).toInstant(), na, 50)
                        ));
    }

    private void mockProjectedBacklog() {
        when(backlogProjection.execute(any()))
                .thenReturn(
                        new ProjectedBacklog(List.of(
                                new BacklogProjectionResponse(WAVING, of(
                                        new ProjectionValue(DATES.get(2), 300),
                                        new ProjectionValue(DATES.get(3), 500)
                                )),
                                new BacklogProjectionResponse(PICKING, of(
                                        new ProjectionValue(DATES.get(2), 300),
                                        new ProjectionValue(DATES.get(3), 500)
                                ))
                        )));
    }

    private void mockTargetBacklog() {
        when(planningModelGateway.getPerformedProcessing(any()))
                .thenReturn(List.of(
                        MagVarPhoto.builder()
                                .date(DATES.get(0))
                                .value(10)
                                .build(),
                        MagVarPhoto.builder()
                                .date(DATES.get(1))
                                .value(15)
                                .build(),
                        MagVarPhoto.builder()
                                .date(DATES.get(2))
                                .value(30)
                                .build(),
                        MagVarPhoto.builder()
                                .date(DATES.get(3))
                                .value(60)
                                .build()
                ));
    }

    private void mockThroughput(ProcessName process) {
        final GetThroughputInput request = GetThroughputInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(List.of(process))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();

        when(getProcessThroughput.execute(request))
                .thenReturn(new GetThroughputResult(
                        Map.of(process, Map.of(
                                DATES.get(0), 10,
                                DATES.get(1), 25,
                                DATES.get(2), 15,
                                DATES.get(3), 5)
                        )
                ));
    }

    private void mockHistoricalBacklog(ProcessName process) {
        final var firstDateHash = 5820;
        final var secondDateHash = 5880;
        final var thirdDateHash = 5940;
        final var fourthDateHash = 6000;

        final var input = new GetHistoricalBacklogInput(
                DATE_CURRENT.toInstant(),
                WAREHOUSE_ID,
                of("outbound-orders"),
                of(process),
                DATE_FROM.toInstant(),
                DATE_TO.toInstant()
        );

        when(getHistoricalBacklog.execute(input))
                .thenReturn(Map.of(
                        WAVING, new HistoricalBacklog(
                                Map.of(
                                        firstDateHash, new UnitMeasure(200, 20),
                                        secondDateHash, new UnitMeasure(100, 10),
                                        thirdDateHash, new UnitMeasure(50, 5),
                                        fourthDateHash, new UnitMeasure(80, 8)
                                )
                        ),
                        PICKING, new HistoricalBacklog(
                                Map.of(
                                        firstDateHash, new UnitMeasure(22, 2),
                                        secondDateHash, new UnitMeasure(111, 11),
                                        thirdDateHash, new UnitMeasure(150, 15),
                                        fourthDateHash, new UnitMeasure(215, 21)
                                )
                        ),
                        PACKING, new HistoricalBacklog(
                                Map.of(
                                        firstDateHash, new UnitMeasure(0, 0),
                                        secondDateHash, new UnitMeasure(120, 12),
                                        thirdDateHash, new UnitMeasure(220,22),
                                        fourthDateHash, new UnitMeasure(420, 42)
                                )
                        )
                ));
    }

    private void mockBacklogLimits(ProcessName process) {
        final var input = GetBacklogLimitsInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(of(process))
                .dateFrom(DATE_FROM.toInstant())
                .dateTo(DATE_TO.toInstant())
                .build();

        final var result = Map.of(
                DATES.get(0).toInstant(), new BacklogLimit(5, 15),
                DATES.get(1).toInstant(), new BacklogLimit(7, 21),
                DATES.get(2).toInstant(), new BacklogLimit(3, 21),
                DATES.get(3).toInstant(), new BacklogLimit(0, -1)
        );

        when(getBacklogLimits.execute(input)).thenReturn(Map.of(process, result));
    }

    private void mockDateUtils(MockedStatic<DateUtils> mockDt) {
        mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));

        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(0).toInstant())).thenReturn(5820);
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(1).toInstant())).thenReturn(5880);
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(2).toInstant())).thenReturn(5940);
        mockDt.when(() -> DateUtils.minutesFromWeekStart(
                DATES.get(3).toInstant())).thenReturn(6000);
    }

}
