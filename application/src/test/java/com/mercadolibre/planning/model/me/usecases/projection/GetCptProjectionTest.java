package com.mercadolibre.planning.model.me.usecases.projection;

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
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.PROCESSING_TIME;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetCptProjectionTest {

    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final TimeZone TIME_ZONE = getDefault();
    private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

    @InjectMocks
    private GetCptProjection getProjection;

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
    private GetBacklog getBacklog;

    @Test
    void testExecute() {
        // Given
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

        final GetProjectionInputDto input = GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklog.execute(new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID)))
                .thenReturn(mockedBacklog);

        when(planningModelGateway.runProjection(
                createProjectionRequest(mockedBacklog, utcCurrentTime)))
                .thenReturn(mockProjections(utcCurrentTime));

        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
        final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
        final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);

        when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .zoneId(TIME_ZONE.toZoneId())
                        .build()
                )
        )).thenReturn(mockSuggestedWaves(utcDateTimeFrom, utcDateTimeTo));

        when(getEntities.execute(input)).thenReturn(mockComplexTable());
        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        // When
        final Projection projection = getProjection.execute(input);

        // Then
        assertEquals("Proyecciones", projection.getTitle());

        assertSimpleTable(projection.getSimpleTable1(), utcDateTimeFrom, utcDateTimeTo);
        assertEquals(mockComplexTable(), projection.getComplexTable1());
        assertEquals(mockSimpleTable(), projection.getSimpleTable2());
        assertChart(projection.getChart());
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
        final Map<String, Object> column1MultiBatch = (Map<String, Object>) data.get(2).get(
                "column_1");
        assertEquals(MULTI_ORDER_DISTRIBUTION.getTitle(), column1MultiBatch.get("subtitle"));

        final String title = simpleTable.getColumns().get(0).getTitle();
        final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(HOUR_MINUTES_FORMATTER) + "-"
                + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(HOUR_MINUTES_FORMATTER);
        final String expectedTitle = "Sig. hora " + nextHour;
        assertEquals(title, expectedTitle);
    }

    private void assertChart(final Chart chart) {
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
        assertEquals(45, chartData1.getProcessingTime().getValue());
        assertChartTooltip(
                chartData1.getTooltip(),
                cpt1.format(HOUR_MINUTES_FORMATTER),
                "-",
                projectedEndDate1.format(HOUR_MINUTES_FORMATTER),
                "45 minutos",
                null);

        final ZonedDateTime cpt2 = convertToTimeZone(zoneId, CPT_2);
        final ZonedDateTime projectedEndDate2 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(3);
        assertEquals(cpt2.format(DATE_SHORT_FORMATTER), chartData2.getTitle());
        assertEquals(cpt2.format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(projectedEndDate2.format(DATE_FORMATTER), chartData2.getProjectedEndTime());
        assertEquals(240, chartData2.getProcessingTime().getValue());
        assertChartTooltip(
                chartData2.getTooltip(),
                cpt2.format(HOUR_MINUTES_FORMATTER),
                "-",
                projectedEndDate2.format(HOUR_MINUTES_FORMATTER),
                "4 horas",
                null);

        final ZonedDateTime cpt3 = convertToTimeZone(zoneId, CPT_3);
        final ZonedDateTime projectedEndDate3 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(3).plusMinutes(25);
        assertEquals(cpt3.format(DATE_SHORT_FORMATTER), chartData3.getTitle());
        assertEquals(cpt3.format(DATE_FORMATTER), chartData3.getCpt());
        assertEquals(projectedEndDate3.format(DATE_FORMATTER), chartData3.getProjectedEndTime());
        assertEquals(240, chartData3.getProcessingTime().getValue());
        assertChartTooltip(
                chartData3.getTooltip(),
                cpt3.format(HOUR_MINUTES_FORMATTER),
                "100",
                projectedEndDate3.format(HOUR_MINUTES_FORMATTER),
                "4 horas",
                null);

        final ZonedDateTime cpt4 = convertToTimeZone(zoneId, CPT_4);
        final ZonedDateTime projectedEndDate4 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(8).plusMinutes(10);
        assertEquals(cpt4.format(DATE_SHORT_FORMATTER), chartData4.getTitle());
        assertEquals(cpt4.format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(projectedEndDate4.format(DATE_FORMATTER), chartData4.getProjectedEndTime());
        assertEquals(250, chartData4.getProcessingTime().getValue());
        assertChartTooltip(
                chartData4.getTooltip(),
                cpt4.format(HOUR_MINUTES_FORMATTER),
                "180",
                projectedEndDate4.format(HOUR_MINUTES_FORMATTER),
                "4 horas y 10 minutos",
                null);

        final ZonedDateTime cpt5 = convertToTimeZone(zoneId, CPT_5);
        final ZonedDateTime projectedEndDate5 = convertToTimeZone(zoneId,
                getCurrentUtcDate().plusDays(1));
        assertEquals(cpt5.format(DATE_SHORT_FORMATTER), chartData5.getTitle());
        assertEquals(cpt5.format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(projectedEndDate5.format(DATE_FORMATTER), chartData5.getProjectedEndTime());
        assertEquals(300, chartData5.getProcessingTime().getValue());
        assertChartTooltip(
                chartData5.getTooltip(),
                cpt5.format(HOUR_MINUTES_FORMATTER),
                "100",
                "Excede las 24hs",
                "5 horas",
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

    private ProjectionRequest createProjectionRequest(final List<Backlog> backlogs,
                                                      final ZonedDateTime currentTime) {
        return ProjectionRequest.builder()
                .processName(List.of(PICKING, PACKING, PACKING_WALL))
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .type(ProjectionType.CPT)
                .backlog(backlogs)
                .applyDeviation(true)
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

    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(CPT_1, 150),
                new Backlog(CPT_2, 235),
                new Backlog(CPT_3, 300),
                new Backlog(CPT_4, 120)
        );
    }

    private List<Backlog> mockSales() {
        return List.of(
                new Backlog(CPT_1, 350),
                new Backlog(CPT_2, 235),
                new Backlog(CPT_3, 200),
                new Backlog(CPT_4, 120)
        );
    }

    private ConfigurationRequest createConfigurationRequest() {
        return  ConfigurationRequest
                .builder()
                .warehouseId(WAREHOUSE_ID)
                .key(PROCESSING_TIME)
                .build();
    }

    private Optional<ConfigurationResponse> mockProcessingTimeConfiguration() {
        return Optional.ofNullable(ConfigurationResponse.builder()
                .metricUnit(MINUTES)
                .value(60)
                .build());
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

