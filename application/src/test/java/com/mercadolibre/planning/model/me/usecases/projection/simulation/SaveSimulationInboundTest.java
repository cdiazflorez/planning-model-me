package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaveSimulationInboundTest  {

    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final TimeZone TIME_ZONE =
            TimeZone.getTimeZone("America/Argentina/Buenos_Aires");

    @InjectMocks
    private SaveSimulationInbound saveSimulationInbound;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetEntities getEntities;

    @Mock
    private GetProjectionSummary getProjectionSummary;

    @Mock
    private GetBacklogByDateInbound getBacklog;

    @Test
    public void testExecuteSaveInbound() {
        // Given
        final ZonedDateTime utcCurrentTime = getCurrentTime();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklog.execute(new GetBacklogByDateDto(FBM_WMS_INBOUND, WAREHOUSE_ID,
                utcCurrentTime.toInstant(), utcCurrentTime.plusDays(4).toInstant())))
                .thenReturn(mockedBacklog);

        when(planningModelGateway.saveSimulation(
                createSimulationRequest(mockedBacklog, utcCurrentTime)))
                .thenReturn(mockProjections(utcCurrentTime));

        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
        final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);

        when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());
        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        // When
        final Projection projection = saveSimulationInbound.execute(GetProjectionInputDto.builder()
                .date(utcDateTimeFrom)
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .simulations(List.of(new Simulation(PUT_AWAY,
                        List.of(new SimulationEntity(
                                HEADCOUNT, List.of(new QuantityByDate(utcCurrentTime, 20))
                        )))))
                .build()
        );

        // Then
        final ZonedDateTime currentTime = utcCurrentTime.withZoneSameInstant(TIME_ZONE.toZoneId());
        assertEquals("Proyecciones", projection.getTitle());

        final Chart chart = projection.getData().getChart();
        final List<ChartData> chartData = chart.getData();

        thenTestCharDataIsOk(currentTime, chartData);

        assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
        assertEquals(mockSimpleTable(), projection.getData().getSimpleTable2());
    }

    private void thenTestCharDataIsOk(final ZonedDateTime currentTime,
                                      final List<ChartData> chartData) {
        assertEquals(5, chartData.size());

        final ChartData chartData1 = chartData.get(0);
        assertEquals(currentTime.plusHours(4).format(DATE_SHORT_FORMATTER),
                chartData1.getTitle());
        assertEquals(currentTime.plusHours(4).format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(currentTime.plusHours(2).plusMinutes(35).format(DATE_FORMATTER),
                chartData1.getProjectedEndTime());
        assertEquals(0, chartData1.getProcessingTime().getValue());

        final ChartData chartData2 = chartData.get(1);
        assertEquals(currentTime.plusHours(7).format(DATE_SHORT_FORMATTER),
                chartData2.getTitle());
        assertEquals(currentTime.plusHours(7).format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(currentTime.plusHours(3).format(DATE_FORMATTER),
                chartData2.getProjectedEndTime());
        assertEquals(0, chartData2.getProcessingTime().getValue());

        final ChartData chartData3 = chartData.get(2);
        assertEquals(
                currentTime.plusHours(5).plusMinutes(30)
                        .format(DATE_SHORT_FORMATTER),
                chartData3.getTitle()
        );
        assertEquals(currentTime.plusHours(5).plusMinutes(30).format(DATE_FORMATTER),
                chartData3.getCpt());
        assertEquals(currentTime.plusHours(3).plusMinutes(20).format(DATE_FORMATTER),
                chartData3.getProjectedEndTime());
        assertEquals(0, chartData3.getProcessingTime().getValue());

        final ChartData chartData4 = chartData.get(3);
        assertEquals(currentTime.plusHours(6).format(DATE_SHORT_FORMATTER),
                chartData4.getTitle()
        );
        assertEquals(currentTime.plusHours(6).format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(currentTime.plusHours(8).plusMinutes(11).format(DATE_FORMATTER),
                chartData4.getProjectedEndTime());
        assertEquals(0, chartData4.getProcessingTime().getValue());

        final ChartData chartData5 = chartData.get(4);
        assertEquals(currentTime.plusHours(8).format(DATE_SHORT_FORMATTER),
                chartData5.getTitle()
        );
        assertEquals(currentTime.plusHours(8).format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(currentTime.plusDays(1).plusHours(1).format(DATE_FORMATTER),
                chartData5.getProjectedEndTime());
        assertEquals(0, chartData5.getProcessingTime().getValue());
    }

    private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
        return List.of(
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(4))
                        .projectedEndDate(utcCurrentTime.plusHours(2).plusMinutes(30))
                        .simulatedEndDate(utcCurrentTime.plusHours(2).plusMinutes(35))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(0, null))
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(7))
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .simulatedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(30)
                        .processingTime(new ProcessingTime(0,null))
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(5).plusMinutes(30))
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
                        .remainingQuantity(50)
                        .processingTime(new ProcessingTime(0, null))
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(6))
                        .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
                        .simulatedEndDate(utcCurrentTime.plusHours(8).plusMinutes(11))
                        .remainingQuantity(180)
                        .processingTime(new ProcessingTime(0, null))
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(8))
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(0, null))
                        .build()
        );
    }

    private SimulationRequest createSimulationRequest(final List<Backlog> backlogs,
                                                      final ZonedDateTime currentTime) {
        return SimulationRequest.builder()
                .processName(List.of(PUT_AWAY))
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(4))
                .backlog(backlogs.stream()
                        .map(backlog -> new QuantityByDate(
                                backlog.getDate(),
                                backlog.getQuantity()))
                        .collect(toList()))
                .simulations(List.of(new Simulation(PUT_AWAY,
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
