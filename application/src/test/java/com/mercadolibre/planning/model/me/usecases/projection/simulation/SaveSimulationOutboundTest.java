package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockPlanningBacklog;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link SaveSimulationOutbound}.
 */
@ExtendWith(MockitoExtension.class)
public class SaveSimulationOutboundTest {

  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

  @InjectMocks
  private SaveSimulationOutbound saveSimulationOutbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private GetWaveSuggestion getWaveSuggestion;

  @Mock
  private GetEntities getEntities;

  @Mock
  private GetSales getSales;

  @Mock
  private GetSimpleDeferralProjection getSimpleDeferralProjection;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Mock
  private FeatureSwitches featureSwitches;

  @Test
  @DisplayName("Execute the case when all the data is correctly generated")
  public void testExecute() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentTime();
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);

    final List<Backlog> mockedBacklog = mockBacklog();
    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(utcCurrentTime);

    final List<String> steps = List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group",
                                       "grouping", "grouped", "to_pack");

    final List<ProcessName> processes = List.of(PACKING, PACKING_WALL);

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(false);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(planningModelGateway.saveSimulation(
        createSimulationRequest(mockedBacklog, utcCurrentTime, processes)))
        .thenReturn(mockProjections(utcCurrentTime));

    when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .zoneId(TIME_ZONE.toZoneId())
        .date(utcDateTimeFrom)
        .build()
                                   )
    )).thenReturn(mockSuggestedWaves(utcDateTimeFrom, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        utcDateTimeFrom,
        mockBacklog(),
        false,
        emptyList())))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjections(utcCurrentTime),
            new LogisticCenterConfiguration(TIME_ZONE)));

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        List.of("outbound-orders"),
        steps,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(4).toInstant(),
        List.of("date_out"))
    ).thenReturn(List.of(
        new Consolidation(null, Map.of("date_out", getCurrentTime().minusHours(1).toString()), 150, true),
        new Consolidation(null, Map.of("date_out", getCurrentTime().plusHours(2).toString()), 235, true),
        new Consolidation(null, Map.of("date_out", getCurrentTime().plusHours(3).toString()), 300, true)
    ));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedPlanningBacklog);

    // When
    final PlanningView planningView = saveSimulationOutbound.execute(GetProjectionInputDto.builder()
                                                                         .date(utcDateTimeFrom)
                                                                         .workflow(FBM_WMS_OUTBOUND)
                                                                         .warehouseId(WAREHOUSE_ID)
                                                                         .simulations(List.of(new Simulation(PICKING,
                                                                                                             List.of(new SimulationEntity(
                                                                                                                 HEADCOUNT, List.of(
                                                                                                                 new QuantityByDate(
                                                                                                                     utcCurrentTime, 20))
                                                                                                             )))))
                                                                         .requestDate(utcCurrentTime.toInstant())
                                                                         .build()
    );


    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertProjection(planningView.getData().getProjections());
  }

  @Test
  @DisplayName("Execute the case when all the data is correctly generated")
  public void testExecuteEnabled() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentTime();
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);

    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(utcCurrentTime);

    final List<String> steps = List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group",
                                       "grouping", "grouped", "to_pack");

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(true);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .zoneId(TIME_ZONE.toZoneId())
        .date(utcDateTimeFrom)
        .build()
                                   )
    )).thenReturn(mockSuggestedWaves(utcDateTimeFrom, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        utcDateTimeFrom,
        mockBacklog(),
        false,
        emptyList())))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjections(utcCurrentTime),
            new LogisticCenterConfiguration(TIME_ZONE)));

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        List.of("outbound-orders"),
        steps,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(4).toInstant(),
        List.of("date_out"))
    ).thenReturn(List.of(
        new Consolidation(null, Map.of("date_out", getCurrentTime().minusHours(1).toString()), 150, true),
        new Consolidation(null, Map.of("date_out", getCurrentTime().plusHours(2).toString()), 235, true),
        new Consolidation(null, Map.of("date_out", getCurrentTime().plusHours(3).toString()), 300, true)
    ));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedPlanningBacklog);

    // When
    final PlanningView planningView =
        saveSimulationOutbound.execute(GetProjectionInputDto.builder()
                                           .date(utcDateTimeFrom)
                                           .workflow(FBM_WMS_OUTBOUND)
                                           .warehouseId(WAREHOUSE_ID)
                                           .simulations(List.of(new Simulation(PICKING,
                                                                               List.of(new SimulationEntity(
                                                                                   HEADCOUNT,
                                                                                   List.of(new QuantityByDate(utcCurrentTime, 20))
                                                                               )))))
                                           .requestDate(utcCurrentTime.toInstant())
                                           .build()
        );


    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
  }

  private void assertProjection(final List<Projection> projections) {
    final ZonedDateTime currentTime = getCurrentUtcDate();

    final Projection projection1 = projections.get(0);
    final ZonedDateTime cpt1 = currentTime.plusHours(8);
    assertProjectionData(projection1, cpt1, null);

    final Projection projection2 = projections.get(1);
    final ZonedDateTime cpt2 = currentTime.plusHours(7);
    final ZonedDateTime projectedEndDate2 = currentTime.plusHours(3);
    assertProjectionData(projection2, cpt2, projectedEndDate2);

    final Projection projection3 = projections.get(2);
    final ZonedDateTime cpt3 = currentTime.plusHours(6);
    final ZonedDateTime projectedEndDate3 = currentTime.plusHours(8).plusMinutes(10);
    assertProjectionData(projection3, cpt3, projectedEndDate3);

    final Projection projection4 = projections.get(3);
    final ZonedDateTime cpt4 = currentTime.plusHours(5).plusMinutes(30);
    final ZonedDateTime projectedEndDate4 = currentTime.plusHours(3).plusMinutes(25);
    assertProjectionData(projection4, cpt4, projectedEndDate4);

    final Projection projection5 = projections.get(4);
    final ZonedDateTime cpt5 = currentTime.plusHours(4);
    final ZonedDateTime projectedEndDate5 = currentTime.plusHours(2).plusMinutes(30);
    assertProjectionData(projection5, cpt5, projectedEndDate5);
  }

  private void assertProjectionData(final Projection projection,
                                    final ZonedDateTime cpt,
                                    final ZonedDateTime projectedEndDate) {
    final Instant projectEnd = projectedEndDate != null ? projectedEndDate.toInstant() : null;

    assertEquals(cpt.toInstant(), projection.getCpt());
    assertEquals(projectEnd, projection.getProjectedEndDate());
  }

  private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
    return List.of(
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(4))
            .projectedEndDate(utcCurrentTime.plusHours(2).plusMinutes(30))
            .simulatedEndDate(utcCurrentTime.plusHours(2).plusMinutes(35))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(7))
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .simulatedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(30)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(5).plusMinutes(30))
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
            .remainingQuantity(50)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(6))
            .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
            .simulatedEndDate(utcCurrentTime.plusHours(8).plusMinutes(11))
            .remainingQuantity(180)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(8))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(300, MINUTES.getName()))
            .build()
    );
  }

  private SimulationRequest createSimulationRequest(final List<Backlog> backlogs,
                                                    final ZonedDateTime currentTime,
                                                    final List<ProcessName> processes) {
    return SimulationRequest.builder()
        .processName(processes)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(currentTime)
        .dateTo(currentTime.plusDays(4))
        .backlog(backlogs.stream()
                     .map(backlog -> new QuantityByDate(
                         backlog.getDate(),
                         backlog.getQuantity()))
                     .collect(toList()))
        .simulations(List.of(new Simulation(PICKING,
                                            List.of(new SimulationEntity(
                                                HEADCOUNT, List.of(new QuantityByDate(currentTime, 20))
                                            )))))
        .applyDeviation(true)
        .timeZone("America/Argentina/Buenos_Aires")
        .build();
  }

  private List<Backlog> mockBacklog() {
    final ZonedDateTime currentTime = getCurrentTime();

    return List.of(
        new Backlog(currentTime.minusHours(1), 150),
        new Backlog(currentTime.plusHours(2), 235),
        new Backlog(currentTime.plusHours(3), 300)
    );
  }

  private ZonedDateTime getCurrentTime() {
    return now(UTC).withMinute(0).withSecond(0).withNano(0);
  }

  private SimpleTable mockSuggestedWaves(final ZonedDateTime utcDateTimeFrom,
                                         final ZonedDateTime utcDateTimeTo) {
    final String title = "Ondas sugeridas";
    final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER) + "-"
        + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER);
    final String expectedTitle = "Sig. hora " + nextHour;
    final List<ColumnHeader> columnHeaders = List.of(
        new ColumnHeader("column_1", expectedTitle, null),
        new ColumnHeader("column_2", "Tamaño de onda", null)
    );
    final List<Map<String, Object>> data = List.of(
        Map.of("column_1",
               Map.of("title", "Unidades por onda", "subtitle",
                      MONO_ORDER_DISTRIBUTION.getTitle()),
               "column_2", "0 uds."
        ),
        Map.of("column_1",
               Map.of("title", "Unidades por onda", "subtitle",
                      MULTI_BATCH_DISTRIBUTION.getTitle()),
               "column_2", "100 uds."
        ),
        Map.of("column_1",
               Map.of("title", "Unidades por onda", "subtitle",
                      MULTI_ORDER_DISTRIBUTION.getTitle()),
               "column_2", "100 uds."
        )
    );
    return new SimpleTable(title, columnHeaders, data);
  }

  private ComplexTable mockComplexTable() {
    return new ComplexTable(
        emptyList(),
        emptyList(),
        new ComplexTableAction("applyLabel", "cancelLabel", "editLabel"),
        "title"
    );
  }
}
