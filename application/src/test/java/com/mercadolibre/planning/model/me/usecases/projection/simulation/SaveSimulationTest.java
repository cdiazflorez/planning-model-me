package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
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
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaveSimulationTest {

    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final TimeZone TIME_ZONE = getDefault();

    @InjectMocks
    private SaveSimulation saveSimulation;

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
    @DisplayName("Execute the case when all the data is correctly generated")
    public void testExecute() {
        // Given
        final ZonedDateTime utcCurrentTime = getCurrentTime();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklog.execute(new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID)))
                .thenReturn(mockedBacklog);

        when(planningModelGateway.saveSimulation(
                createSimulationRequest(mockedBacklog, utcCurrentTime)))
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

        when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());
        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        // When
        final Projection projection = saveSimulation.execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .simulations(List.of(new Simulation(PICKING,
                        List.of(new SimulationEntity(
                                HEADCOUNT, List.of(new QuantityByDate(utcCurrentTime, 20))
                        )))))
                .build()
        );

        // Then
        final ZonedDateTime currentTime = utcCurrentTime.withZoneSameInstant(TIME_ZONE.toZoneId());
        assertEquals("Proyecciones", projection.getTitle());

        final Chart chart = projection.getChart();
        final List<ChartData> chartData = chart.getData();

        thenTestCharDataIsOk(currentTime, chart, chartData);

        assertEquals(mockComplexTable(), projection.getComplexTable1());
        assertEquals(mockSimpleTable(), projection.getSimpleTable2());
    }

    private void thenTestCharDataIsOk(ZonedDateTime currentTime,
                                      Chart chart,
                                      List<ChartData> chartData) {
        assertEquals(60, chart.getProcessingTime().getValue());
        assertEquals(5, chartData.size());

        final ChartData chartData1 = chartData.get(0);
        assertEquals(currentTime.plusHours(4).format(DATE_SHORT_FORMATTER),
                chartData1.getTitle());
        assertEquals(currentTime.plusHours(4).format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(currentTime.plusHours(2).plusMinutes(35).format(DATE_FORMATTER),
                chartData1.getProjectedEndTime());

        final ChartData chartData2 = chartData.get(1);
        assertEquals(currentTime.plusHours(7).format(DATE_SHORT_FORMATTER),
                chartData2.getTitle());
        assertEquals(currentTime.plusHours(7).format(DATE_FORMATTER), chartData2.getCpt());
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
        assertEquals(currentTime.plusHours(8).format(DATE_SHORT_FORMATTER),
                chartData5.getTitle()
        );
        assertEquals(currentTime.plusHours(8).format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(currentTime.plusDays(1).format(DATE_FORMATTER),
                chartData5.getProjectedEndTime());
    }

    private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
        return List.of(
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(4))
                        .projectedEndDate(utcCurrentTime.plusHours(2).plusMinutes(30))
                        .simulatedEndDate(utcCurrentTime.plusHours(2).plusMinutes(35))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(7))
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .simulatedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(30)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(5).plusMinutes(30))
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
                        .remainingQuantity(50)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(6))
                        .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
                        .simulatedEndDate(utcCurrentTime.plusHours(8).plusMinutes(11))
                        .remainingQuantity(180)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(8))
                        .remainingQuantity(100)
                        .build()
        );
    }

    private SimulationRequest createSimulationRequest(final List<Backlog> backlogs,
                                                      final ZonedDateTime currentTime) {
        return SimulationRequest.builder()
                .processName(List.of(PICKING, PACKING, PACKING_WALL))
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
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
                .format(ofPattern("HH:mm")) + "-"
                + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
                .format(ofPattern("HH:mm"));
        final String expextedTitle = "Sig. hora " + nextHour;
        final List<ColumnHeader> columnHeaders = List.of(
                new ColumnHeader("column_1", expextedTitle, null),
                new ColumnHeader("column_2", "Tamaño de onda", null)
        );
        final List<Map<String, Object>> data = List.of(
                Map.of("column_1",
                        Map.of("title","Unidades por onda","subtitle",
                                MONO_ORDER_DISTRIBUTION.getTitle()),
                        "column_2", "0 uds."
                ),
                Map.of("column_1",
                        Map.of("title","Unidades por onda","subtitle",
                                MULTI_BATCH_DISTRIBUTION.getTitle()),
                        "column_2", "100 uds."
                ),
                Map.of("column_1",
                        Map.of("title","Unidades por onda","subtitle",
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
