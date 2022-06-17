package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
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

/**
 * Tests {@link RunSimulationOutbound}.
 */
@ExtendWith(MockitoExtension.class)
public class RunSimulationOutboundTest {

    private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

    @InjectMocks
    private RunSimulationOutbound runSimulationOutbound;

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
    public void testExecute() {
        // Given
        final ZonedDateTime utcCurrentTime = getCurrentTime();

        final List<String> steps = List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group",
                "grouping", "grouped", "to_pack");

        final List<ProcessName> processes = List.of(PACKING, PACKING_WALL);

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        final List<Backlog> mockedBacklog = mockBacklog();

        when(planningModelGateway.runSimulation(
                createSimulationRequest(mockedBacklog, utcCurrentTime, processes)))
                .thenReturn(mockProjections(utcCurrentTime));

        final ZonedDateTime utcDateTimeTo = utcCurrentTime.plusHours(1);

        when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .zoneId(TIME_ZONE.toZoneId())
                        .date(utcCurrentTime)
                        .build()
                )
        )).thenReturn(mockSuggestedWaves(utcCurrentTime, utcDateTimeTo));

        when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());
        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                utcCurrentTime,
                mockBacklog(),
                false,
                null)))
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

        // When
        final Projection projection = runSimulationOutbound.execute(GetProjectionInputDto.builder()
                .date(utcCurrentTime)
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .simulations(List.of(new Simulation(PICKING, List.of(new SimulationEntity(
                        HEADCOUNT, List.of(new QuantityByDate(utcCurrentTime, 20))
                )))))
                .requestDate(utcCurrentTime.toInstant())
                .build()
        );

        // Then
        final ZonedDateTime currentTime = utcCurrentTime.withZoneSameInstant(TIME_ZONE.toZoneId());
        assertEquals("Proyecciones", projection.getTitle());

        final Chart chart = projection.getData().getChart();
        final List<ChartData> chartData = chart.getData();

        assertEquals(5, chartData.size());

        final ChartData chartData1 = chartData.get(0);
        assertEquals(currentTime.plusHours(4).format(DATE_SHORT_FORMATTER),
                chartData1.getTitle());
        assertEquals(currentTime.plusHours(4).format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(currentTime.plusHours(2).plusMinutes(35).format(DATE_FORMATTER),
                chartData1.getProjectedEndTime());

        final ChartData chartData2 = chartData.get(1);
        assertEquals(currentTime.plusHours(5).format(DATE_SHORT_FORMATTER),
                chartData2.getTitle());
        assertEquals(currentTime.plusHours(5).format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(currentTime.plusHours(3).format(DATE_FORMATTER),
                chartData2.getProjectedEndTime());

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

        final ChartData chartData4 = chartData.get(3);
        assertEquals(currentTime.plusHours(6).format(DATE_SHORT_FORMATTER),
                chartData4.getTitle()
        );
        assertEquals(currentTime.plusHours(6).format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(currentTime.plusHours(8).plusMinutes(11).format(DATE_FORMATTER),
                chartData4.getProjectedEndTime());

        final ChartData chartData5 = chartData.get(4);
        assertEquals(currentTime.plusHours(7).format(DATE_SHORT_FORMATTER),
                chartData5.getTitle()
        );
        assertEquals(currentTime.plusHours(7).format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(currentTime.plusDays(1).format(DATE_FORMATTER),
                chartData5.getProjectedEndTime());

        assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
        assertEquals(mockSimpleTable(), projection.getData().getSimpleTable2());
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
                        .date(utcCurrentTime.plusHours(5))
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .simulatedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(5).plusMinutes(30))
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
                        .remainingQuantity(0)
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
                        .date(utcCurrentTime.plusHours(7))
                        .remainingQuantity(100)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
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
                .simulations(List.of(new Simulation(PICKING, List.of(new SimulationEntity(
                        HEADCOUNT, List.of(new QuantityByDate(currentTime, 20))
                )))))
                .applyDeviation(true)
                .timeZone(BA_ZONE)
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

    private SimpleTable mockSimpleTable() {
        return new SimpleTable(
                "title",
                emptyList(),
                emptyList()
        );
    }
}
