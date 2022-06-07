package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.TOOLTIP_DATE_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartTooltip;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests {@link GetSlaProjectionOutbound}. */
@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionOutboundTest {

  private static final List<String> STATUSES = List.of("pending", "to_route", "to_pick", "picked", "to_sort",
      "sorted", "to_group", "grouping", "grouped", "to_pack");

  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");

  private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

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
  private GetProjectionSummary getProjectionSummary;

  @Mock
  private GetSimpleDeferralProjection getSimpleDeferralProjection;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Test
  void testOutboundExecute() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(4);

    final List<ProcessName> processes = List.of(PACKING, PACKING_WALL);
    final List<Integer> processingTimes = List.of(45, 240, 240, 250, 300);
    final List<String> subtitles = List.of("45 minutos", "4 horas", "4 horas", "4 horas y 10 minutos", "5 horas");

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(currentUtcDateTime)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    when(planningModelGateway.runProjection(
        createProjectionRequestOutbound(mockedBacklog, processes, currentUtcDateTime, utcDateTimeTo))
    ).thenReturn(mockProjections(currentUtcDateTime));

    when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .zoneId(TIME_ZONE.toZoneId())
            .date(currentUtcDateTime)
            .build()
        )
    )).thenReturn(mockSuggestedWaves(currentUtcDateTime, utcDateTimeTo));

    when(getEntities.execute(input)).thenReturn(mockComplexTable());
    when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
        .thenReturn(mockSimpleTable());

    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        currentUtcDateTime,
        mockBacklog(),
        false, null)))
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

    // When
    final Projection projection = getSlaProjectionOutbound.execute(input);

    // Then
    assertEquals("Proyecciones", projection.getTitle());

    assertSimpleTable(projection.getData().getSimpleTable1(), currentUtcDateTime, utcDateTimeTo);
    assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
    assertEquals(mockSimpleTable(), projection.getData().getSimpleTable2());
    assertChart(projection.getData().getChart(), processingTimes, subtitles);
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
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)
    ));

    // When
    final Projection projection = getSlaProjectionOutbound.execute(input);

    // Then
    assertEquals("Proyecciones", projection.getTitle());
    assertEquals(1, projection.getTabs().size());
    assertNull(projection.getData());
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

  private void assertChart(final Chart chart,
                           final List<Integer> processingTimes,
                           final List<String> subtitles) {

    final ZoneId zoneId = TIME_ZONE.toZoneId();
    final List<ChartData> chartData = chart.getData();
    final ChartData chartData1 = chartData.get(0);
    final ChartData chartData2 = chartData.get(1);
    final ChartData chartData3 = chartData.get(2);
    final ChartData chartData4 = chartData.get(3);
    final ChartData chartData5 = chartData.get(4);

    assertEquals(5, chartData.size());
    final ZonedDateTime cpt1 = convertToTimeZone(zoneId, CPT_1);
    final ZonedDateTime projectedEndDate1 = convertToTimeZone(zoneId,
        getCurrentUtcDate()).plusHours(3).plusMinutes(30);

    assertEquals(cpt1.format(DATE_SHORT_FORMATTER), chartData1.getTitle());
    assertEquals(cpt1.format(DATE_FORMATTER), chartData1.getCpt());
    assertEquals(projectedEndDate1.format(DATE_FORMATTER), chartData1.getProjectedEndTime());
    assertEquals(processingTimes.get(0), chartData1.getProcessingTime().getValue());
    assertChartTooltip(
        chartData1.getTooltip(),
        cpt1.format(HOUR_MINUTES_FORMATTER),
        "-",
        projectedEndDate1.format(TOOLTIP_DATE_FORMATTER),
        subtitles.get(0),
        null);

    final ZonedDateTime cpt2 = convertToTimeZone(zoneId, CPT_2);
    final ZonedDateTime projectedEndDate2 = convertToTimeZone(zoneId,
        getCurrentUtcDate()).plusHours(3);
    assertEquals(cpt2.format(DATE_SHORT_FORMATTER), chartData2.getTitle());
    assertEquals(cpt2.format(DATE_FORMATTER), chartData2.getCpt());
    assertEquals(projectedEndDate2.format(DATE_FORMATTER), chartData2.getProjectedEndTime());
    assertEquals(processingTimes.get(1), chartData2.getProcessingTime().getValue());
    assertChartTooltip(
        chartData2.getTooltip(),
        cpt2.format(HOUR_MINUTES_FORMATTER),
        "-",
        projectedEndDate2.format(TOOLTIP_DATE_FORMATTER),
        subtitles.get(1),
        null);

    final ZonedDateTime cpt3 = convertToTimeZone(zoneId, CPT_3);
    final ZonedDateTime projectedEndDate3 = convertToTimeZone(zoneId,
        getCurrentUtcDate()).plusHours(3).plusMinutes(25);
    assertEquals(cpt3.format(DATE_SHORT_FORMATTER), chartData3.getTitle());
    assertEquals(cpt3.format(DATE_FORMATTER), chartData3.getCpt());
    assertEquals(projectedEndDate3.format(DATE_FORMATTER), chartData3.getProjectedEndTime());
    assertEquals(processingTimes.get(2), chartData3.getProcessingTime().getValue());
    assertChartTooltip(
        chartData3.getTooltip(),
        cpt3.format(HOUR_MINUTES_FORMATTER),
        "100",
        projectedEndDate3.format(TOOLTIP_DATE_FORMATTER),
        subtitles.get(2),
        null);

    final ZonedDateTime cpt4 = convertToTimeZone(zoneId, CPT_4);
    final ZonedDateTime projectedEndDate4 = convertToTimeZone(zoneId,
        getCurrentUtcDate()).plusHours(8).plusMinutes(10);
    assertEquals(cpt4.format(DATE_SHORT_FORMATTER), chartData4.getTitle());
    assertEquals(cpt4.format(DATE_FORMATTER), chartData4.getCpt());
    assertEquals(projectedEndDate4.format(DATE_FORMATTER), chartData4.getProjectedEndTime());
    assertEquals(processingTimes.get(3), chartData4.getProcessingTime().getValue());
    assertChartTooltip(
        chartData4.getTooltip(),
        cpt4.format(HOUR_MINUTES_FORMATTER),
        "180",
        projectedEndDate4.format(TOOLTIP_DATE_FORMATTER),
        subtitles.get(3),
        null);

    final ZonedDateTime cpt5 = convertToTimeZone(zoneId, CPT_5);
    final ZonedDateTime projectedEndDate5 = convertToTimeZone(zoneId,
        getCurrentUtcDate().plusDays(1));
    assertEquals(cpt5.format(DATE_SHORT_FORMATTER), chartData5.getTitle());
    assertEquals(cpt5.format(DATE_FORMATTER), chartData5.getCpt());
    assertEquals(projectedEndDate5.format(DATE_FORMATTER), chartData5.getProjectedEndTime());
    assertEquals(processingTimes.get(4), chartData5.getProcessingTime().getValue());
    assertChartTooltip(
        chartData5.getTooltip(),
        cpt5.format(HOUR_MINUTES_FORMATTER),
        "100",
        "Excede las 24hs",
        subtitles.get(4),
        "Diferido");
  }

  private void assertChartTooltip(final ChartTooltip tooltip,
                                  final String subtitle1,
                                  final String subtitle2,
                                  final String subtitle3,
                                  final String subtitle4,
                                  final String title5) {
    assertEquals("CPT:", tooltip.getTitle1());
    assertEquals(subtitle1, tooltip.getSubtitle1());
    assertEquals("Desviación:", tooltip.getTitle2());
    assertEquals(subtitle2, tooltip.getSubtitle2());
    assertEquals("Cierre proyectado:", tooltip.getTitle3());
    assertEquals(subtitle3, tooltip.getSubtitle3());
    assertEquals("Cycle time:", tooltip.getTitle4());
    assertEquals(subtitle4, tooltip.getSubtitle4());
    assertEquals(title5, tooltip.getTitle5());
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

  private SimpleTable mockSimpleTable() {
    return new SimpleTable(
        "title",
        emptyList(),
        emptyList()
    );
  }
}

