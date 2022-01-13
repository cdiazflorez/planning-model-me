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
import java.time.temporal.ChronoUnit;
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
import static java.time.temporal.ChronoUnit.HOURS;
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
    private static final ZonedDateTime SLA_6 = getCurrentUtcDate().minusHours(2);

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
        final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(4);
        final GetProjectionInputDto input = GetProjectionInputDto.builder()
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklogByDateInbound.execute(new GetBacklogByDateDto(FBM_WMS_INBOUND, WAREHOUSE_ID,
                currentUtcDateTime.toInstant(), utcDateTimeTo.toInstant())))
                .thenReturn(mockedBacklog);

        when(planningModelGateway.runProjection(
                createProjectionRequestInbound(mockedBacklog, currentUtcDateTime, utcDateTimeTo)))
                .thenReturn(mockProjections(currentUtcDateTime));

        when(getEntities.execute(input)).thenReturn(mockComplexTable());
        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class))).thenReturn(mockSimpleTable());


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
                new Backlog(SLA_4, 120),
                new Backlog(SLA_6, 50)
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
                        .projectedEndDate(null)
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(true)
                        .build(),
                ProjectionResult.builder()
                        .date(SLA_6)
                        .projectedEndDate(utcCurrentTime.plusMinutes(15))
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(0, MINUTES.getName()))
                        .isDeferred(true)
                        .build()
        );
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
        final ZonedDateTime currentDate = getCurrentUtcDate();
        final List<ChartData> chartData = chart.getData();
        assertEquals(6, chartData.size());

        final ChartData chartData1 = chartData.get(0);
        final ZonedDateTime cpt1 = convertToTimeZone(zoneId, SLA_1);
        final ZonedDateTime projectedEndDate1 = convertToTimeZone(zoneId, currentDate).plusHours(3).plusMinutes(30);

        assertEquals(cpt1.format(DATE_SHORT_FORMATTER), chartData1.getTitle());
        assertEquals(cpt1.format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(projectedEndDate1.format(DATE_FORMATTER), chartData1.getProjectedEndTime());
        assertEquals(0, chartData1.getProcessingTime().getValue());
        assertChartTooltip(
                chartData1.getTooltip(),
                cpt1.format(HOUR_MINUTES_FORMATTER),
                "-",
                projectedEndDate1.format(HOUR_MINUTES_FORMATTER));

        final ChartData chartData2 = chartData.get(1);
        final ZonedDateTime cpt2 = convertToTimeZone(zoneId, SLA_2);
        final ZonedDateTime projectedEndDate2 = convertToTimeZone(zoneId, currentDate).plusHours(3);

        assertEquals(cpt2.format(DATE_SHORT_FORMATTER), chartData2.getTitle());
        assertEquals(cpt2.format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(projectedEndDate2.format(DATE_FORMATTER), chartData2.getProjectedEndTime());
        assertEquals(0, chartData2.getProcessingTime().getValue());
        assertChartTooltip(
                chartData2.getTooltip(),
                cpt2.format(HOUR_MINUTES_FORMATTER),
                "-",
                projectedEndDate2.format(HOUR_MINUTES_FORMATTER));

        final ChartData chartData3 = chartData.get(2);
        final ZonedDateTime cpt3 = convertToTimeZone(zoneId, SLA_3);
        final ZonedDateTime projectedEndDate3 = convertToTimeZone(zoneId, currentDate).plusHours(3).plusMinutes(25);

        assertEquals(cpt3.format(DATE_SHORT_FORMATTER), chartData3.getTitle());
        assertEquals(cpt3.format(DATE_FORMATTER), chartData3.getCpt());
        assertEquals(projectedEndDate3.format(DATE_FORMATTER), chartData3.getProjectedEndTime());
        assertEquals(0, chartData3.getProcessingTime().getValue());
        assertChartTooltip(
                chartData3.getTooltip(),
                cpt3.format(HOUR_MINUTES_FORMATTER),
                "100",
                projectedEndDate3.format(HOUR_MINUTES_FORMATTER));

        final ChartData chartData4 = chartData.get(3);
        final ZonedDateTime cpt4 = convertToTimeZone(zoneId, SLA_4);
        final ZonedDateTime projectedEndDate4 = convertToTimeZone(zoneId, currentDate).plusHours(8).plusMinutes(10);

        assertEquals(cpt4.format(DATE_SHORT_FORMATTER), chartData4.getTitle());
        assertEquals(cpt4.format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(projectedEndDate4.format(DATE_FORMATTER), chartData4.getProjectedEndTime());
        assertEquals(0, chartData4.getProcessingTime().getValue());
        assertChartTooltip(
                chartData4.getTooltip(),
                cpt4.format(HOUR_MINUTES_FORMATTER),
                "180",
                projectedEndDate4.format(HOUR_MINUTES_FORMATTER));

        final ChartData chartData5 = chartData.get(4);
        final ZonedDateTime cpt5 = convertToTimeZone(zoneId, SLA_5);
        final ZonedDateTime projectedEndDate5 = convertToTimeZone(zoneId, currentDate.plusDays(1));

        assertEquals(cpt5.format(DATE_SHORT_FORMATTER), chartData5.getTitle());
        assertEquals(cpt5.format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(projectedEndDate5.format(DATE_FORMATTER), chartData5.getProjectedEndTime());
        assertEquals(0, chartData5.getProcessingTime().getValue());
        assertChartTooltip(
                chartData5.getTooltip(),
                cpt5.format(HOUR_MINUTES_FORMATTER),
                "100",
                "Excede las 24hs");

        final ChartData chartData6 = chartData.get(5);
        final ZonedDateTime cpt6 = convertToTimeZone(zoneId, SLA_6);
        final ZonedDateTime projectedEndDate6 = convertToTimeZone(zoneId, currentDate.plusMinutes(15));
        assertEquals(cpt6.format(DATE_SHORT_FORMATTER), chartData6.getTitle());
        assertEquals(convertToTimeZone(zoneId, currentDate).format(DATE_FORMATTER), chartData6.getCpt());
        assertEquals(projectedEndDate6.format(DATE_FORMATTER), chartData6.getProjectedEndTime());
        assertEquals(0, chartData6.getProcessingTime().getValue());
        assertChartTooltip(
                chartData6.getTooltip(),
                cpt6.format(HOUR_MINUTES_FORMATTER),
                "100",
                projectedEndDate6.format(HOUR_MINUTES_FORMATTER));
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


