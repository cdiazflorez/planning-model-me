package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
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

import com.mercadolibre.planning.model.me.entities.monitor.DetailedBacklogPhoto;
import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogMonitorDetailsInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import com.mercadolibre.planning.model.me.usecases.throughput.GetProcessThroughput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetBacklogMonitorDetailsTest {
  private static final List<ZonedDateTime> DATES = of(
      parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T02:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T02:25:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T03:00:00Z", ISO_OFFSET_DATE_TIME),
      parse("2021-08-12T04:00:00Z", ISO_OFFSET_DATE_TIME)
  );

  private static final ZonedDateTime DATE_CURRENT = DATES.get(2);

  private static final ZonedDateTime DATE_FROM = DATES.get(0);

  private static final ZonedDateTime DATE_TO = DATES.get(4);

  @InjectMocks
  private GetBacklogMonitorDetails getBacklogMonitor;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private GetProcessThroughput getProcessThroughput;

  @Mock
  private GetHistoricalBacklog getHistoricalBacklog;

  @Mock
  private List<GetBacklogMonitorDetails.BacklogProvider> backlogProviders;

  @Mock
  private GetBacklogMonitorDetails.BacklogProvider backlogProvider;

  private MockedStatic<DateUtils> mockDt;

  private static GetBacklogMonitorDetailsInput input(final ProcessName processName) {
    return new GetBacklogMonitorDetailsInput(
        DATE_CURRENT.toInstant(),
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        processName,
        DATE_FROM.toInstant(),
        DATE_TO.toInstant(),
        999L,
        false
    );
  }

  @BeforeEach
  void setUp() {
    when(backlogProvider.canProvide(Mockito.any())).thenReturn(true);
    when(backlogProviders.stream()).thenReturn(Stream.of(backlogProvider));
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

    final GetBacklogMonitorDetailsInput input = input(PICKING);

    mockPickingBacklog(input);
    mockThroughput(FBM_WMS_OUTBOUND, of(WAVING, PICKING));
    mockHistoricalBacklog(FBM_WMS_OUTBOUND, PICKING);

    // WHEN
    final var result = getBacklogMonitor.execute(input);

    // THEN
    assertNotNull(result);
    assertEquals(5, result.getDates().size());

    final var lastHourDetails = result.getDates().get(4);
    assertEquals(215, lastHourDetails.getTotal().getUnits());
    assertEquals(109, lastHourDetails.getTotal().getMinutes());
    assertEquals(18, lastHourDetails.getHeadcount().getAbsolute());
    assertEquals(5, lastHourDetails.getAreas().size());

    final var mz = lastHourDetails.getAreas().get(2);
    assertEquals(180, mz.getValue().getUnits());
    assertEquals(92, mz.getValue().getMinutes());
    assertEquals(12, mz.getHeadcount().getAbsolute());
    assertEquals(0.66, mz.getHeadcount().getPercentage());

    assertEquals(2, mz.getSubareas().size());

    assertEquals("MZ-1", mz.getSubareas().get(0).getId());
    assertEquals(80, mz.getSubareas().get(0).getValue().getUnits());
    assertEquals(41, mz.getSubareas().get(0).getValue().getMinutes());
    assertEquals(2, mz.getSubareas().get(0).getHeadcount().getAbsolute());
    assertEquals(0.11, mz.getSubareas().get(0).getHeadcount().getPercentage());

    assertEquals("MZ-2", mz.getSubareas().get(1).getId());
    assertEquals(100, mz.getSubareas().get(1).getValue().getUnits());
    assertEquals(51, mz.getSubareas().get(1).getValue().getMinutes());
    assertEquals(10, mz.getSubareas().get(1).getHeadcount().getAbsolute());
    assertEquals(0.55, mz.getSubareas().get(1).getHeadcount().getPercentage());
  }

  @Test
  void testGetBacklogDetailsWithoutTargets() {
    // GIVEN
    mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));

    GetBacklogMonitorDetailsInput input = input(PACKING);

    mockBacklogWithoutAreas();
    mockThroughput(FBM_WMS_OUTBOUND, of(PACKING));
    mockHistoricalBacklog(FBM_WMS_OUTBOUND, PACKING);

    // WHEN
    var response = getBacklogMonitor.execute(input);

    // THEN
    var results = response.getDates();
    assertEquals(5, results.size());

    verify(planningModelGateway, never()).getPerformedProcessing(any());
  }

  @Test
  void testGetBacklogDetailsWithoutAreas() {
    // GIVEN
    mockDt.when(DateUtils::getCurrentUtcDateTime).thenReturn(DATES.get(1));

    final GetBacklogMonitorDetailsInput input = input(WAVING);

    mockBacklogWithoutAreas();
    mockTargetBacklog();
    mockThroughput(FBM_WMS_OUTBOUND, of(WAVING));
    mockHistoricalBacklog(FBM_WMS_OUTBOUND, WAVING);

    // WHEN
    final var response = getBacklogMonitor.execute(input);

    // THEN
    final var results = response.getDates();
    assertEquals(5, results.size());

    assertResultWithoutArea(results.get(0), DATES.get(0).toInstant(), 28, 103, 10, 38);
    assertResultWithoutArea(results.get(1), DATES.get(1).toInstant(), 30, 80, 15, 41);
    assertResultWithoutArea(results.get(2), DATES.get(2).toInstant(), 50, 120, 15, 36);
    assertResultWithoutArea(results.get(3), DATES.get(3).toInstant(), 60, 114, 30, 59);
    assertResultWithoutArea(results.get(4), DATES.get(4).toInstant(), 100, 75, 60, 45);

    final var graph = response.getProcess();
    assertEquals("waving", graph.getProcess());
    assertEquals(50, graph.getTotal().getUnits());
    assertEquals(120, graph.getTotal().getMinutes());
    assertEquals(5, graph.getBacklogs().size());
  }

  private void assertResultWithoutArea(final DetailedBacklogPhoto photo,
                                       final Instant date,
                                       final Integer totalUnits,
                                       final Integer totalMinutes,
                                       final Integer targetUnits,
                                       final Integer targetMinutes) {

    assertEquals(date, photo.getDate());
    assertNull(photo.getAreas());

    var totals = photo.getTotal();
    assertEquals(totalUnits, totals.getUnits());
    assertEquals(totalMinutes, totals.getMinutes());

    var targets = photo.getTarget();
    assertEquals(targetUnits, targets.getUnits());
    assertEquals(targetMinutes, targets.getMinutes());
  }

  private void mockPickingBacklog(final GetBacklogMonitorDetailsInput input) {

    when(backlogProvider.getMonitorBacklog(Mockito.any()))
        .thenReturn(
            Map.of(
                DATES.get(0).toInstant(), of(
                    new NumberOfUnitsInAnArea("RK", 15),
                    new NumberOfUnitsInAnArea("HV", 75)
                ),
                DATES.get(1).toInstant(), of(
                    new NumberOfUnitsInAnArea("RK", 50),
                    new NumberOfUnitsInAnArea("HV", 0)
                ),
                DATES.get(2).toInstant(), of(
                    new NumberOfUnitsInAnArea("RK", 50),
                    new NumberOfUnitsInAnArea("HV", 0)
                ),
                DATES.get(3).toInstant(), of(
                    new NumberOfUnitsInAnArea(
                        "BL", of(new NumberOfUnitsInASubarea("BL-0", 40, 3, 0.17)), 3, 0.17
                    ),
                    new NumberOfUnitsInAnArea(
                        "MZ",
                        of(
                            new NumberOfUnitsInASubarea("MZ-1", 30, 4, 0.23),
                            new NumberOfUnitsInASubarea("MZ-2", 10, 2, 0.11)
                        ),
                        6,
                        0.34
                    ),
                    new NumberOfUnitsInAnArea(
                        "NA", of(new NumberOfUnitsInASubarea("NA", 60, 8, 0.47)), 8, 0.47
                    )
                ),
                DATES.get(4).toInstant(), of(
                    new NumberOfUnitsInAnArea(
                        "BL", of(new NumberOfUnitsInASubarea("BL-0", 20, 3, 0.16)), 3, 0.16
                    ),
                    new NumberOfUnitsInAnArea(
                        "MZ",
                        of(
                            new NumberOfUnitsInASubarea("MZ-1", 80, 2, 0.11),
                            new NumberOfUnitsInASubarea("MZ-2", 100, 10, 0.55)
                        ),
                        12,
                        0.66
                    ),
                    new NumberOfUnitsInAnArea(
                        "NA", of(new NumberOfUnitsInASubarea("NA", 15, 3, 0.16)), 3, 0.16
                    )
                )
            )
        );
  }

  private void mockBacklogWithoutAreas() {
    when(backlogProvider.getMonitorBacklog(Mockito.any()))
        .thenReturn(
            Map.of(
                DATES.get(0).toInstant(), of(
                    new NumberOfUnitsInAnArea("N/A", 28)
                ),
                DATES.get(1).toInstant(), of(
                    new NumberOfUnitsInAnArea("N/A", 30)
                ),
                DATES.get(2).toInstant(), of(
                    new NumberOfUnitsInAnArea("N/A", 50)
                ),
                DATES.get(3).toInstant(), of(
                    new NumberOfUnitsInAnArea("N/A", 60)
                ),
                DATES.get(4).toInstant(), of(
                    new NumberOfUnitsInAnArea("N/A", 100)
                )
            )
        );
  }

  private void mockTargetBacklog() {
    when(planningModelGateway.getPerformedProcessing(any()))
        .thenReturn(of(
            MagnitudePhoto.builder()
                .date(DATES.get(0))
                .value(10)
                .build(),
            MagnitudePhoto.builder()
                .date(DATES.get(1))
                .value(15)
                .build(),
            MagnitudePhoto.builder()
                .date(DATES.get(3))
                .value(30)
                .build(),
            MagnitudePhoto.builder()
                .date(DATES.get(4))
                .value(60)
                .build()
        ));
  }

  private void mockThroughput(final Workflow workflow, final List<ProcessName> processes) {
    final GetThroughputInput request = GetThroughputInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(workflow)
        .processes(processes)
        .dateFrom(DATE_CURRENT)
        .dateTo(DATE_TO.plusHours(24))
        .build();

    final Map<ProcessName, Map<ZonedDateTime, Integer>> result = processes.stream()
        .collect(Collectors.toMap(
                     Function.identity(),
                     process -> Map.of(
                         DATES.get(0), 10,
                         DATES.get(1), 25,
                         DATES.get(3), 15,
                         DATES.get(4), 50,
                         DATES.get(4).plusHours(1L), 200)
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

}
