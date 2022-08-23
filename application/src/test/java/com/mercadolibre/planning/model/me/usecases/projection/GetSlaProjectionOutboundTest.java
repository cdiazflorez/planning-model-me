package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link GetSlaProjectionOutbound}.
 */
@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionOutboundTest {

  private static final List<String> STATUSES = List.of("pending", "to_route", "to_pick", "picked", "to_sort",
      "sorted", "to_group", "grouping", "grouped", "to_pack");

  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

  private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);

  private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);

  private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);

  private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);

  private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

  @InjectMocks
  private GetSlaProjectionOutbound getSlaProjectionOutbound;

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
  void testOutboundExecute() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(4);

    final List<ProcessName> processes = List.of(PACKING, PACKING_WALL);
    final List<Integer> processingTimes = List.of(300, 250, 240, 240, 45);

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(currentUtcDateTime)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(false);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    when(planningModelGateway.runProjection(
        createProjectionRequestOutbound(mockedBacklog, processes, currentUtcDateTime, utcDateTimeTo))
    ).thenReturn(mockProjections(currentUtcDateTime));

    when(getWaveSuggestion.execute(any(GetWaveSuggestionInputDto.class)))
        .thenReturn(mockSuggestedWaves(currentUtcDateTime, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionsDeferral(currentUtcDateTime),
            new LogisticCenterConfiguration(TIME_ZONE)));

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        List.of("outbound-orders"),
        STATUSES,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(4).toInstant(),
        List.of("date_out"))
    ).thenReturn(List.of(
        new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
        new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
        new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true),
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)
    ));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertProjection(planningView.getData().getProjections(), processingTimes);
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertSimpleTable(planningView.getData().getSimpleTable1(), currentUtcDateTime, utcDateTimeTo);
  }

  @Test
  void testOutboundExecuteEnabled() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(4);

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(currentUtcDateTime)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(true);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    when(getWaveSuggestion.execute(any(GetWaveSuggestionInputDto.class)))
        .thenReturn(mockSuggestedWaves(currentUtcDateTime, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionsDeferral(currentUtcDateTime),
            new LogisticCenterConfiguration(TIME_ZONE)));

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        List.of("outbound-orders"),
        STATUSES,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(4).toInstant(),
        List.of("date_out"))
    ).thenReturn(List.of(
        new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
        new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
        new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true),
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)
    ));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertSimpleTable(planningView.getData().getSimpleTable1(), currentUtcDateTime, utcDateTimeTo);
  }

  @Test
  void testExecuteWithError() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime;
    final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusDays(4);
    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(utcDateTimeFrom)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    final List<ProcessName> processes = List.of(PICKING, PACKING, PACKING_WALL);
    when(planningModelGateway.runProjection(
        createProjectionRequestOutbound(mockedBacklog, processes, utcDateTimeFrom, utcDateTimeTo)))
        .thenThrow(RuntimeException.class);

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        List.of("outbound-orders"),
        STATUSES,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(4).toInstant(),
        List.of("date_out"))
    ).thenReturn(List.of(
        new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
        new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
        new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true),
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)));

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getData());
  }

  private void assertProjection(final List<Projection> projections,
                                final List<Integer> processingTimes) {
    final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

    assertEquals(5, projections.size());

    final Projection projection1 = projections.get(0);
    final ZonedDateTime cpt1 = CPT_1.plusHours(3);
    assertProjectionData(projection1, cpt1, null, processingTimes.get(0), 100);

    final Projection projection2 = projections.get(1);
    final ZonedDateTime cpt2 = CPT_2.plusHours(1);
    final ZonedDateTime projectedEndDate2 = utcCurrentTime.plusHours(8).plusMinutes(10);
    assertProjectionData(projection2, cpt2, projectedEndDate2, processingTimes.get(1), 180);

    final Projection projection3 = projections.get(2);
    final ZonedDateTime projectedEndDate3 = utcCurrentTime.plusHours(3).plusMinutes(25);
    assertProjectionData(projection3, CPT_3, projectedEndDate3, processingTimes.get(2), 100);

    final Projection projection4 = projections.get(3);
    final ZonedDateTime cpt4 = CPT_4.minusHours(1);
    final ZonedDateTime projectedEndDate4 = utcCurrentTime.plusHours(3);
    assertProjectionData(projection4, cpt4, projectedEndDate4, processingTimes.get(3), 0);

    final Projection projection5 = projections.get(4);
    final ZonedDateTime cpt5 = CPT_5.minusHours(3);
    final ZonedDateTime projectedEndDate5 = utcCurrentTime.plusHours(3).plusMinutes(30);
    assertProjectionData(projection5, cpt5, projectedEndDate5, processingTimes.get(4), 0);
  }

  private void assertProjectionData(final Projection projection,
                                    final ZonedDateTime cpt,
                                    final ZonedDateTime projectedEndDate,
                                    final int processingTime,
                                    final int remainingQuantity) {
    final Instant endDate = projectedEndDate != null ? projectedEndDate.toInstant() : null;

    assertEquals(cpt.toInstant(), projection.getCpt());
    assertEquals(processingTime, projection.getCycleTime());
    assertEquals(remainingQuantity, projection.getRemainingQuantity());
    assertEquals(endDate, projection.getProjectedEndDate());
  }

  private void assertSimpleTable(final SimpleTable simpleTable,
                                 final ZonedDateTime utcDateTimeFrom,
                                 final ZonedDateTime utcDateTimeTo) {
    List<Map<String, Object>> data = simpleTable.getData();
    assertEquals(3, data.size());
    assertEquals("0 uds.", data.get(0).get("column_2"));
    final Map<String, Object> column1Mono = (Map<String, Object>) data.get(0).get("column_1");
    assertEquals(MONO_ORDER_DISTRIBUTION.getTitle(), column1Mono.get("subtitle"));

    assertEquals("100 uds.", data.get(1).get("column_2"));
    final Map<String, Object> column1Multi = (Map<String, Object>) data.get(1).get("column_1");
    assertEquals(MULTI_BATCH_DISTRIBUTION.getTitle(), column1Multi.get("subtitle"));

    assertEquals("100 uds.", data.get(1).get("column_2"));
    final Map<String, Object> column1MultiBatch = (Map<String, Object>) data.get(2).get("column_1");
    assertEquals(MULTI_ORDER_DISTRIBUTION.getTitle(), column1MultiBatch.get("subtitle"));

    final String title = simpleTable.getColumns().get(0).getTitle();
    final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER) + "-"
        + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER);
    final String expectedTitle = "Sig. hora " + nextHour;
    assertEquals(title, expectedTitle);
  }

  private ProjectionRequest createProjectionRequestOutbound(final List<Backlog> backlogs,
                                                            final List<ProcessName> processes,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo) {
    return ProjectionRequest.builder()
        .processName(processes)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .type(ProjectionType.CPT)
        .backlog(backlogs)
        .applyDeviation(true)
        .timeZone(BA_ZONE)
        .build();
  }

  private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
    return List.of(
        ProjectionResult.builder()
            .date(CPT_1)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(45, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_2)
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_3)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_4)
            .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
            .remainingQuantity(180)
            .processingTime(new ProcessingTime(250, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_5)
            .projectedEndDate(null)
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(300, MINUTES.getName()))
            .isDeferred(true)
            .build()
    );
  }

  private List<ProjectionResult> mockProjectionsDeferral(ZonedDateTime utcCurrentTime) {
    return List.of(
        ProjectionResult.builder()
            .date(CPT_1)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(45, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_2)
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_3)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_4)
            .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
            .remainingQuantity(180)
            .processingTime(new ProcessingTime(250, MINUTES.getName()))
            .isDeferred(false)
            .build()
    );
  }

  private List<Backlog> mockBacklog() {
    return List.of(
        new Backlog(CPT_1, 150),
        new Backlog(CPT_2, 235),
        new Backlog(CPT_3, 300),
        new Backlog(CPT_4, 120)
    );
  }

  private SimpleTable mockSuggestedWaves(final ZonedDateTime utcDateTimeFrom,
                                         final ZonedDateTime utcDateTimeTo) {
    final String title = "Ondas sugeridas";
    final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER) + "-"
        + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER);
    final String expextedTitle = "Sig. hora " + nextHour;
    final List<ColumnHeader> columnHeaders = List.of(
        new ColumnHeader("column_1", expextedTitle, null),
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
