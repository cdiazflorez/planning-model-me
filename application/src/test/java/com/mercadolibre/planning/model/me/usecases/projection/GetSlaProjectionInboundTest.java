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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
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
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionInboundTest {

    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final TimeZone TIME_ZONE =
            TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
    private static final ZonedDateTime SLA_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime SLA_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime SLA_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime SLA_4 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime SLA_5 = getCurrentUtcDate().plusHours(7);

    @InjectMocks
    private GetSlaProjectionInbound getSlaProjectionInbound;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetEntities getEntities;

    @Mock
    private GetProjectionSummary getProjectionSummary;
    @Mock
    private GetBacklogByDateInbound getBacklogByDateInbound;


    @Test
    void testInboundExecute() {
        // Given
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
        final ZonedDateTime utcDateTimeFrom = currentUtcDateTime;
        final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusDays(4);

        final GetProjectionInputDto input = GetProjectionInputDto.builder()
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .date(utcDateTimeFrom)
                .build();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklogByDateInbound.execute(new GetBacklogByDateDto(FBM_WMS_INBOUND, WAREHOUSE_ID,
                utcDateTimeFrom.toInstant(), utcDateTimeTo.toInstant())))
                .thenReturn(mockedBacklog);

        when(planningModelGateway.runProjection(
                createProjectionRequestInbound(mockedBacklog, utcDateTimeFrom, utcDateTimeTo)))
                .thenReturn(mockProjections(utcDateTimeFrom));

        when(getEntities.execute(input)).thenReturn(mockComplexTable());
        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        // When
        final Projection projection = getSlaProjectionInbound.execute(input);

        // Then
        assertEquals("Proyecciones", projection.getTitle());


        assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
        assertEquals(mockSimpleTable(), projection.getData().getSimpleTable2());
        assertChart(projection.getData().getChart());
    }


    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(SLA_1, 150),
                new Backlog(SLA_2, 235),
                new Backlog(SLA_3, 300),
                new Backlog(SLA_4, 120)
        );
    }


    private ProjectionRequest createProjectionRequestInbound(final List<Backlog> backlogs,
                                                             final ZonedDateTime dateFrom,
                                                             final ZonedDateTime dateTo) {
        return ProjectionRequest.builder()
                .processName(List.of(PUT_AWAY))
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .type(ProjectionType.CPT)
                .backlog(backlogs)
                .applyDeviation(true)
                .timeZone("America/Argentina/Buenos_Aires")
                .build();
    }

    private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
        return List.of(
                ProjectionResult.builder()
                        .date(SLA_1)
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(SLA_2)
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(SLA_3)
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(SLA_4)
                        .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
                        .remainingQuantity(180)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(SLA_5)
                        .isExpired(true)
                        .projectedEndDate(null)
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(true)
                        .build()
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

    private void assertChart(final Chart chart) {
        final ZoneId zoneId = TIME_ZONE.toZoneId();
        final List<ChartData> chartData = chart.getData();
        final ChartData chartData1 = chartData.get(0);
        final ChartData chartData2 = chartData.get(1);
        final ChartData chartData3 = chartData.get(2);
        final ChartData chartData4 = chartData.get(3);
        final ChartData chartData5 = chartData.get(4);

        assertEquals(5, chartData.size());
        final ZonedDateTime cpt1 = convertToTimeZone(zoneId, SLA_1);
        final ZonedDateTime projectedEndDate1 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(3).plusMinutes(30);

        assertEquals(cpt1.format(DATE_SHORT_FORMATTER), chartData1.getTitle());
        assertEquals(cpt1.format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(projectedEndDate1.format(DATE_FORMATTER), chartData1.getProjectedEndTime());
        assertEquals(0, chartData1.getProcessingTime().getValue());
        assertChartTooltip(
                chartData1.getTooltip(),
                cpt1.format(HOUR_MINUTES_FORMATTER),
                "-",
                projectedEndDate1.format(HOUR_MINUTES_FORMATTER));

        final ZonedDateTime cpt2 = convertToTimeZone(zoneId, SLA_2);
        final ZonedDateTime projectedEndDate2 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(3);
        assertEquals(cpt2.format(DATE_SHORT_FORMATTER), chartData2.getTitle());
        assertEquals(cpt2.format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(projectedEndDate2.format(DATE_FORMATTER), chartData2.getProjectedEndTime());
        assertEquals(0, chartData2.getProcessingTime().getValue());
        assertChartTooltip(
                chartData2.getTooltip(),
                cpt2.format(HOUR_MINUTES_FORMATTER),
                "-",
                projectedEndDate2.format(HOUR_MINUTES_FORMATTER));

        final ZonedDateTime cpt3 = convertToTimeZone(zoneId, SLA_3);
        final ZonedDateTime projectedEndDate3 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(3).plusMinutes(25);
        assertEquals(cpt3.format(DATE_SHORT_FORMATTER), chartData3.getTitle());
        assertEquals(cpt3.format(DATE_FORMATTER), chartData3.getCpt());
        assertEquals(projectedEndDate3.format(DATE_FORMATTER), chartData3.getProjectedEndTime());
        assertEquals(0, chartData3.getProcessingTime().getValue());
        assertChartTooltip(
                chartData3.getTooltip(),
                cpt3.format(HOUR_MINUTES_FORMATTER),
                "100",
                projectedEndDate3.format(HOUR_MINUTES_FORMATTER));

        final ZonedDateTime cpt4 = convertToTimeZone(zoneId, SLA_4);
        final ZonedDateTime projectedEndDate4 = convertToTimeZone(zoneId,
                getCurrentUtcDate()).plusHours(8).plusMinutes(10);
        assertEquals(cpt4.format(DATE_SHORT_FORMATTER), chartData4.getTitle());
        assertEquals(cpt4.format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(projectedEndDate4.format(DATE_FORMATTER), chartData4.getProjectedEndTime());
        assertEquals(0, chartData4.getProcessingTime().getValue());
        assertChartTooltip(
                chartData4.getTooltip(),
                cpt4.format(HOUR_MINUTES_FORMATTER),
                "180",
                projectedEndDate4.format(HOUR_MINUTES_FORMATTER));

        final ZonedDateTime cpt5 = convertToTimeZone(zoneId, SLA_5);
        final ZonedDateTime projectedEndDate5 = convertToTimeZone(zoneId,
                getCurrentUtcDate().plusDays(1));
        assertEquals(cpt5.format(DATE_SHORT_FORMATTER), chartData5.getTitle());
        assertEquals(cpt5.format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(projectedEndDate5.format(DATE_FORMATTER), chartData5.getProjectedEndTime());
        assertEquals(0, chartData5.getProcessingTime().getValue());
        assertChartTooltip(
                chartData5.getTooltip(),
                cpt5.format(HOUR_MINUTES_FORMATTER),
                "100",
                "Excede las 24hs");
    }

    private void assertChartTooltip(final ChartTooltip tooltip,
                                    final String subtitle1,
                                    final String subtitle2,
                                    final String subtitle3) {
        assertEquals("SLA:", tooltip.getTitle1());
        assertEquals(subtitle1, tooltip.getSubtitle1());
        assertEquals("Desviación:", tooltip.getTitle2());
        assertEquals(subtitle2, tooltip.getSubtitle2());
        assertEquals("Cierre proyectado:", tooltip.getTitle3());
        assertEquals(subtitle3, tooltip.getSubtitle3());
    }
}


