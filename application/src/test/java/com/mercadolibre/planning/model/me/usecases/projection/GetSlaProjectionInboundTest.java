package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartTooltip;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.DATE_FORMATTER;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.DATE_ONLY_FORMATTER;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.DATE_SHORT_FORMATTER;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.TIME_ZONE;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.TOOLTIP_DATE_FORMATTER;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockComplexTable;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockSimpleTable;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionInboundTest {

    private static final String OVERDUE_TAG = " - Vencidos";

    private static final ZonedDateTime SLA_1 = getCurrentUtcDate().minusHours(2);
    private static final ZonedDateTime SLA_2 = getCurrentUtcDate().minusHours(1);
    private static final ZonedDateTime SLA_3 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime SLA_4 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime SLA_5 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
    private static final ZonedDateTime SLA_6 = getCurrentUtcDate().plusHours(6);
    private static final ZonedDateTime SLA_7 = getCurrentUtcDate().plusHours(7);

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

        final List<Backlog> mockedBacklog = mockBacklog();
        final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog();

        final GetProjectionInputDto input = GetProjectionInputDto.builder()
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .requestDate(Instant.now())
                .build();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));


        when(getBacklogByDateInbound.execute(new GetBacklogByDateDto(FBM_WMS_INBOUND, WAREHOUSE_ID,
                currentUtcDateTime.toInstant(), utcDateTimeTo.toInstant())))
                .thenReturn(mockedBacklog);

        when(planningModelGateway.runProjection(
                createProjectionRequestInbound(mockedPlanningBacklog, currentUtcDateTime, utcDateTimeTo)))
                .thenReturn(mockProjections(currentUtcDateTime));

        when(getEntities.execute(input)).thenReturn(mockComplexTable());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class))).thenReturn(mockSimpleTable());

        // When
        final Projection projection = getSlaProjectionInbound.execute(input);

        // Then
        assertNull(projection.getEmptyStateMessage());

        assertEquals("Proyecciones", projection.getTitle());
        assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
        assertEquals(mockSimpleTable(), projection.getData().getSimpleTable2());
        assertChart(projection.getData().getChart());
    }

    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(SLA_1, 50),
                new Backlog(SLA_2, -30),
                new Backlog(SLA_3, 150),
                new Backlog(SLA_4, 235),
                new Backlog(SLA_5, 300),
                new Backlog(SLA_6, 120)
        );
    }

    private List<Backlog> mockPlanningBacklog() {
        final ZonedDateTime truncatedDate = convertToTimeZone(TIME_ZONE.toZoneId(), SLA_1)
                .truncatedTo(ChronoUnit.DAYS);

        final ZonedDateTime sla1 = convertToTimeZone(ZoneId.of("Z"), truncatedDate);

        return List.of(
                new Backlog(sla1, 50),
                new Backlog(SLA_3, 150),
                new Backlog(SLA_4, 235),
                new Backlog(SLA_5, 300),
                new Backlog(SLA_6, 120)
        );
    }

    private ProjectionRequest createProjectionRequestInbound(final List<Backlog> backlogs,
                                                             final ZonedDateTime dateFrom,
                                                             final ZonedDateTime dateTo) {
        return ProjectionRequest.builder()
                .processName(List.of(CHECK_IN, PUT_AWAY))
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

    private ProjectionResult mockProjectionResult(final ZonedDateTime date,
                                                  final ZonedDateTime projectedEndDate,
                                                  final int remainingQuantity) {

        return ProjectionResult.builder()
                .date(date)
                .projectedEndDate(projectedEndDate)
                .remainingQuantity(remainingQuantity)
                .processingTime(new ProcessingTime(0, MINUTES.getName()))
                .build();
    }

    private List<ProjectionResult> mockProjections(final ZonedDateTime utcCurrentTime) {
        return List.of(
                mockProjectionResult(SLA_3, utcCurrentTime.plusHours(3).plusMinutes(30), 0),
                mockProjectionResult(SLA_4, utcCurrentTime.plusHours(3), 0),
                mockProjectionResult(SLA_5, utcCurrentTime.plusHours(3).plusMinutes(25), 100),
                mockProjectionResult(SLA_6, utcCurrentTime.plusHours(8).plusMinutes(10), 180),
                mockProjectionResult(SLA_7, null, 100),
                mockProjectionResult(SLA_1, utcCurrentTime.plusMinutes(15), 100)
        );
    }

    private void assertChart(final Chart chart) {
        final ZoneId zoneId = TIME_ZONE.toZoneId();
        final ZonedDateTime currentDate = getCurrentUtcDate();
        final List<ChartData> chartData = chart.getData();
        assertEquals(6, chartData.size());

        final ChartData chartData1 = chartData.get(0);
        final ZonedDateTime cpt1 = convertToTimeZone(zoneId, SLA_3);
        final ZonedDateTime projectedEndDate1 = convertToTimeZone(zoneId, currentDate).plusHours(3).plusMinutes(30);
        assertChartData(chartData1, cpt1, projectedEndDate1, "-", false);

        final ChartData chartData2 = chartData.get(1);
        final ZonedDateTime cpt2 = convertToTimeZone(zoneId, SLA_4);
        final ZonedDateTime projectedEndDate2 = convertToTimeZone(zoneId, currentDate).plusHours(3);
        assertChartData(chartData2, cpt2, projectedEndDate2, "-", false);

        final ChartData chartData3 = chartData.get(2);
        final ZonedDateTime cpt3 = convertToTimeZone(zoneId, SLA_5);
        final ZonedDateTime projectedEndDate3 = convertToTimeZone(zoneId, currentDate).plusHours(3).plusMinutes(25);
        assertChartData(chartData3, cpt3, projectedEndDate3, "100 uds.", false);

        final ChartData chartData4 = chartData.get(3);
        final ZonedDateTime cpt4 = convertToTimeZone(zoneId, SLA_6);
        final ZonedDateTime projectedEndDate4 = convertToTimeZone(zoneId, currentDate).plusHours(8).plusMinutes(10);
        assertChartData(chartData4, cpt4, projectedEndDate4, "180 uds.", false);

        final ChartData chartData5 = chartData.get(4);
        final ZonedDateTime cpt5 = convertToTimeZone(zoneId, SLA_7);
        final ZonedDateTime projectedEndDate5 = convertToTimeZone(zoneId, currentDate.plusDays(1));
        assertChartData(chartData5, cpt5, projectedEndDate5, "100 uds.", true);

        final ChartData chartData6 = chartData.get(5);
        final ZonedDateTime cpt6 = convertToTimeZone(zoneId, SLA_1);
        final ZonedDateTime projectedEndDate6 = convertToTimeZone(zoneId, currentDate.plusMinutes(15));
        assertEquals(cpt6.format(DATE_ONLY_FORMATTER), chartData6.getTitle());
        assertEquals(cpt6.format(DATE_FORMATTER), chartData6.getCpt());
        assertEquals(projectedEndDate6.format(DATE_FORMATTER), chartData6.getProjectedEndTime());
        assertEquals(0, chartData6.getProcessingTime().getValue());
        assertChartTooltip(
                chartData6.getTooltip(),
                cpt6.format(DATE_ONLY_FORMATTER) + OVERDUE_TAG,
                "100 uds.",
                projectedEndDate6.format(TOOLTIP_DATE_FORMATTER));
    }

    private void assertChartData(final ChartData chartData,
                                 final ZonedDateTime cpt,
                                 final ZonedDateTime projectedEndDate,
                                 final String remainingQuantity,
                                 final boolean upTo24hs) {

        assertEquals(cpt.format(DATE_SHORT_FORMATTER), chartData.getTitle());
        assertEquals(cpt.format(DATE_FORMATTER), chartData.getCpt());
        assertEquals(projectedEndDate.format(DATE_FORMATTER), chartData.getProjectedEndTime());
        assertEquals(0, chartData.getProcessingTime().getValue());
        assertChartTooltip(
                chartData.getTooltip(),
                cpt.format(HOUR_MINUTES_FORMATTER),
                remainingQuantity,
                upTo24hs ? "Excede las 24hs" : projectedEndDate.format(TOOLTIP_DATE_FORMATTER));
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


