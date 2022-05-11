package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.CARRY_OVER;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.PROCESSED;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.PROCESS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.services.backlog.BacklogApiAdapter;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetBacklogMonitorDetailsTest {
  private static final List<ZonedDateTime> DATES = of(
      parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T02:15:00Z", ISO_OFFSET_DATE_TIME)
  );

  private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
      FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING),
      FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
  );

  private static final ZonedDateTime DATE_CURRENT = DATES.get(1);

  private static final ZonedDateTime DATE_FROM = DATES.get(0);

  private static final ZonedDateTime DATE_TO = DATES.get(3);

  @InjectMocks
  private GetBacklogMonitorDetails getBacklogMonitor;

  @Mock
  private BacklogApiAdapter backlogApiAdapter;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private GetProcessThroughput getProcessThroughput;

  @Mock
  private GetHistoricalBacklog getHistoricalBacklog;

  @Mock
  private GetBacklogLimits getBacklogLimits;

  @Mock
  private ProjectBacklog projectBacklog;

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
  void testGetPickingBacklogDetails() {
    // GIVEN
    mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));

    GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
        DATE_CURRENT.toInstant(),
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PICKING,
        DATE_FROM.toInstant(),
        DATE_TO.toInstant(),
        999L
    );

    when(projectBacklog.projectBacklogInAreas(
        eq(DATE_CURRENT.toInstant()),
        eq(DATE_TO.toInstant()),
        eq(WAREHOUSE_ID),
        eq(FBM_WMS_OUTBOUND),
        eq(of(WAVING, PICKING)),
        any(),
        any()
    )).thenReturn(of(
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), WAVING, "global", PROCESSED, 10L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), WAVING, "global", CARRY_OVER, 10L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), PICKING, "MZ-1", PROCESSED, 30L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), PICKING, "MZ-2", PROCESSED, 50L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), PICKING, "BL-0", PROCESSED, 50L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), PICKING, "MZ-1", CARRY_OVER, 25L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), PICKING, "MZ-2", CARRY_OVER, 40L),
        new ProjectedBacklogForAnAreaAndOperatingHour(DATE_TO.toInstant(), PICKING, "BL-0", CARRY_OVER, 40L)
    ));

    mockPickingAndWavingPastBacklog(input);
    mockThroughput(FBM_WMS_OUTBOUND, of(WAVING, PICKING));
    mockHistoricalBacklog(FBM_WMS_OUTBOUND, PICKING);
    mockBacklogLimits(FBM_WMS_OUTBOUND, PICKING);

    // WHEN
    final var result = getBacklogMonitor.execute(input);

    // THEN
    assertNotNull(result);
    assertEquals(4, result.getDates().size());

    final var lastHourDetails = result.getDates().get(3);
    final var areas = lastHourDetails.getAreas();
    assertEquals(5, areas.size());
    assertEquals(40, areas.get(0).getValue().getUnits());
    assertEquals(0, areas.get(1).getValue().getUnits());

    final var mz = areas.get(2);
    assertEquals(65, mz.getValue().getUnits());
    assertEquals(2, mz.getSubareas().size());
    assertEquals("MZ-1", mz.getSubareas().get(0).getName());
    assertEquals(25, mz.getSubareas().get(0).getValue().getUnits());
    assertEquals("MZ-2", mz.getSubareas().get(0).getName());
    assertEquals(40, mz.getSubareas().get(0).getValue().getUnits());
  }

  @Test
  void testGetBacklogDetailsWithoutTargets() {
    // GIVEN
    GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
        DATE_CURRENT.toInstant(),
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PACKING,
        DATE_FROM.toInstant(),
        DATE_TO.toInstant(),
        999L
    );

    mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));
    mockPastBacklogWithoutAreas(input);
    mockProjectedBacklog(input);
    mockThroughput(FBM_WMS_OUTBOUND, of(PACKING));
    mockHistoricalBacklog(FBM_WMS_OUTBOUND, PACKING);
    mockBacklogLimits(FBM_WMS_OUTBOUND, PACKING);

    // WHEN
    var response = getBacklogMonitor.execute(input);

    // THEN
    var results = response.getDates();
    assertEquals(4, results.size());

    verify(planningModelGateway, never()).getPerformedProcessing(any());
  }

  @Test
  void testGetBacklogDetailsWithoutAreas() {
    // GIVEN
    final GetBacklogMonitorDetailsInput input = new GetBacklogMonitorDetailsInput(
        DATE_CURRENT.toInstant(),
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        WAVING,
        DATE_FROM.toInstant(),
        DATE_TO.toInstant(),
        999L
    );

    mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));
    mockPastBacklogWithoutAreas(input);
    mockProjectedBacklog(input);
    mockTargetBacklog();
    mockThroughput(FBM_WMS_OUTBOUND, of(WAVING));
    mockHistoricalBacklog(FBM_WMS_OUTBOUND, WAVING);
    mockBacklogLimits(FBM_WMS_OUTBOUND, WAVING);

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
    assertEquals(103, firstTotals.getMinutes());

    var firstTargets = firstResult.getTarget();
    assertEquals(10, firstTargets.getUnits());
    assertEquals(38, firstTargets.getMinutes());

    var lastResults = results.get(3);
    assertEquals(DATE_TO.toInstant(), lastResults.getDate());
    assertNull(lastResults.getAreas());

    assertEquals(60, lastResults.getTarget().getUnits());
    assertEquals(180, lastResults.getTarget().getMinutes());

    assertEquals(500, lastResults.getTotal().getUnits());
    assertEquals(1440, lastResults.getTotal().getMinutes());

    var graph = response.getProcess();
    assertEquals("waving", graph.getProcess());
    assertEquals(50, graph.getTotal().getUnits());
    assertEquals(240, graph.getTotal().getMinutes());
    assertEquals(4, graph.getBacklogs().size());
  }

  private void mockPickingAndWavingPastBacklog(final GetBacklogMonitorDetailsInput input) {
    Instant firstDate = DATES.get(0).toInstant();
    Instant secondDate = DATES.get(1).toInstant();
    Instant dateWithMinutes = DATES.get(4).toInstant();

    var rkH = Map.of(
        "process", "picking",
        "area", "RK"
    );
    var rkL = Map.of(
        "process", "picking",
        "area", "HV"
    );
    var rs = Map.of(
        "process", "picking",
        "area", "RS"
    );
    var waving = Map.of(
        "process", "waving",
        "area", "N/A"
    );

    when(backlogApiAdapter.getCurrentBacklog(
        input.getRequestDate(),
        input.getWarehouseId(),
        of(input.getWorkflow()),
        of(WAVING, PICKING),
        of(PROCESS, AREA, DATE_OUT),
        input.getDateFrom(),
        input.getDateTo(),
        input.getRequestDate(),
        input.getRequestDate().plus(24, ChronoUnit.HOURS))
    ).thenReturn(List.of(
        new Consolidation(firstDate, rkH, 15, true),
        new Consolidation(secondDate, rkH, 50, true),
        new Consolidation(firstDate, rkL, 75, true),
        new Consolidation(dateWithMinutes, rs, 130, false),
        new Consolidation(dateWithMinutes, waving, 550, false)
    ));
  }

  private void mockPastBacklogWithoutAreas(final GetBacklogMonitorDetailsInput input) {
    var na = Map.of("area", "N/A");
    when(backlogApiAdapter.getCurrentBacklog(
        input.getRequestDate(),
        input.getWarehouseId(),
        of(input.getWorkflow()),
        of(input.getProcess()),
        of(input.getProcess().hasAreas() ? AREA : PROCESS),
        input.getDateFrom(),
        input.getDateTo(),
        input.getRequestDate(),
        input.getRequestDate().plus(24, ChronoUnit.HOURS))
    ).thenReturn(List.of(
        new Consolidation(DATES.get(0).toInstant(), na, 28, true),
        new Consolidation(DATES.get(1).toInstant(), na, 50, true)
    ));
  }

  private void mockProjectedBacklog(final GetBacklogMonitorDetailsInput input) {
    Instant secondDate = DATES.get(1).toInstant();
    Instant dateWithMinutes = DATES.get(4).toInstant();

    final Instant dateFrom = input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

    final Instant dateTo = input.getDateTo()
        .truncatedTo(ChronoUnit.HOURS)
        .minus(1L, ChronoUnit.HOURS);

    var na = Map.of("area", "N/A");
    var rs = Map.of("area", "RS");

    final var consolidations = input.getProcess() == PUT_AWAY
        ? of(new Consolidation(dateWithMinutes, rs, 130, false))
        : of(new Consolidation(secondDate, na, 50, true));

    when(
        backlogApiAdapter.getProjectedBacklog(input.getWarehouseId(),
            input.getWorkflow(),
            PROCESS_BY_WORKFLOWS.get(input.getWorkflow()),
            dateFrom.atZone(ZoneId.of("UTC")).withFixedOffsetZone(),
            dateTo.atZone(ZoneId.of("UTC")).withFixedOffsetZone(),
            input.getCallerId(),
            consolidations)
    ).thenReturn(
        List.of(
            new BacklogProjectionResponse(WAVING, of(
                new ProjectionValue(DATES.get(2), 300),
                new ProjectionValue(DATES.get(3), 500)
            )),
            new BacklogProjectionResponse(PICKING, of(
                new ProjectionValue(DATES.get(2), 300),
                new ProjectionValue(DATES.get(3), 500)
            )),
            new BacklogProjectionResponse(PACKING, of(
                new ProjectionValue(DATES.get(2), 300),
                new ProjectionValue(DATES.get(3), 500)
            ))
        )
    );
  }

  private void mockTargetBacklog() {
    when(planningModelGateway.getPerformedProcessing(any()))
        .thenReturn(List.of(
            MagnitudePhoto.builder()
                .date(DATES.get(0))
                .value(10)
                .build(),
            MagnitudePhoto.builder()
                .date(DATES.get(1))
                .value(15)
                .build(),
            MagnitudePhoto.builder()
                .date(DATES.get(2))
                .value(30)
                .build(),
            MagnitudePhoto.builder()
                .date(DATES.get(3))
                .value(60)
                .build()
        ));
  }

  private void mockThroughput(final Workflow workflow, final List<ProcessName> processes) {
    final GetThroughputInput request = GetThroughputInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(workflow)
        .processes(processes)
        .dateFrom(DATE_FROM)
        .dateTo(DATE_TO.plusHours(24))
        .build();

    final Map<ProcessName, Map<ZonedDateTime, Integer>> result = processes.stream()
        .collect(Collectors.toMap(
                Function.identity(),
                process -> Map.of(
                    DATES.get(0), 10,
                    DATES.get(1), 25,
                    DATES.get(2), 15,
                    DATES.get(3), 5)
            )
        );

    when(getProcessThroughput.execute(request)).thenReturn(new GetThroughputResult(result));
  }

  private void mockHistoricalBacklog(final Workflow workflow, final ProcessName process) {
    final var firstDateHash = 5820;
    final var secondDateHash = 5880;
    final var thirdDateHash = 5940;
    final var fourthDateHash = 6000;

    final var input = new GetHistoricalBacklogInput(
        DATE_CURRENT.toInstant(),
        WAREHOUSE_ID,
        workflow,
        of(process),
        DATE_FROM.toInstant(),
        DATE_TO.toInstant()
    );

    final var outboundResults = Map.of(
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
                thirdDateHash, new UnitMeasure(220, 22),
                fourthDateHash, new UnitMeasure(420, 42)
            )
        )
    );

    final var inboundResults = Map.of(
        PUT_AWAY, new HistoricalBacklog(
            Map.of(
                firstDateHash, new UnitMeasure(0, 0),
                secondDateHash, new UnitMeasure(120, 12),
                thirdDateHash, new UnitMeasure(220, 22),
                fourthDateHash, new UnitMeasure(420, 42)
            )
        )
    );

    when(getHistoricalBacklog.execute(input))
        .thenReturn(workflow == FBM_WMS_INBOUND ? inboundResults : outboundResults);
  }

  private void mockBacklogLimits(final Workflow workflow, final ProcessName process) {
    final var input = GetBacklogLimitsInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(workflow)
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
