package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.RowName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.TimeZone.getDefault;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RunSimulationTest {

    private static final DateTimeFormatter HOUR_MINUTES_FORMAT = ofPattern("HH:mm");
    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final TimeZone TIME_ZONE = getDefault();

    @InjectMocks
    private RunSimulation runSimulation;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetBacklog getBacklog;

    @Mock
    private GetSales getSales;

    @Test
    public void testExecute() {
        // Given
        final ZonedDateTime utcCurrentTime = getCurrentTime();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(planningModelGateway.getEntities(createRequest(HEADCOUNT, utcCurrentTime,
                List.of(ProcessingType.ACTIVE_WORKERS))))
                .thenReturn(mockHeadcountEntities(utcCurrentTime));

        when(planningModelGateway.getEntities(createRequest(PRODUCTIVITY, utcCurrentTime, null)))
                .thenReturn(mockProductivityEntities(utcCurrentTime));

        when(planningModelGateway.getEntities(createRequest(THROUGHPUT, utcCurrentTime, null)))
                .thenReturn(mockThroughputEntities());

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklog.execute(new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID)))
                .thenReturn(mockedBacklog);

        when(getSales.execute(new GetSalesInputDto(
                FBM_WMS_OUTBOUND, WAREHOUSE_ID, utcCurrentTime.minusHours(28)))
        ).thenReturn(mockSales());

        when(planningModelGateway.runSimulation(
                createSimulationRequest(mockedBacklog, utcCurrentTime)))
                .thenReturn(mockProjections(utcCurrentTime));

        // When
        final Projection projection = runSimulation.execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .simulations(List.of(new Simulation(PICKING, List.of(new SimulationEntity(
                        HEADCOUNT, List.of(new QuantityByDate(utcCurrentTime, 20))
                )))))
                .build()
        );

        // Then
        final ZonedDateTime currentTime = utcCurrentTime.withZoneSameInstant(TIME_ZONE.toZoneId());
        assertEquals("Proyecciones", projection.getTitle());

        final List<ColumnHeader> columns = projection.getComplexTable1().getColumns();
        final List<Data> data = projection.getComplexTable1().getData();

        IntStream.range(0, 24).forEach(index -> {
            assertEquals("column_" + (index + 2), columns.get(index + 1).getId());
            assertEquals(currentTime.plusHours(index).format(HOUR_FORMAT),
                    columns.get(index + 1).getTitle());
        });
        assertEquals(3, data.size());

        final Data headcount = data.get(0);
        final Data productivity = data.get(1);
        final Data throughput = data.get(2);

        assertEquals(HEADCOUNT.getName(), headcount.getId());
        assertTrue(headcount.isOpen());
        assertEquals(26, headcount.getContent().get(0).size());
        assertEquals(26, headcount.getContent().get(1).size());
        assertEquals("20", headcount.getContent().get(0).get("column_2").getTitle());
        assertEquals(currentTime, headcount.getContent().get(0).get("column_2").getDate()
                .withMinute(0).withSecond(0).withNano(0));
        assertEquals("Cantidad de reps FCST", headcount.getContent().get(0).get("column_2")
                .getTooltip().get("title_2"));
        assertEquals("10", headcount.getContent().get(0).get("column_2")
                .getTooltip().get("subtitle_2"));
        assertEquals("15", headcount.getContent().get(1).get("column_4").getTitle());

        assertEquals(PRODUCTIVITY.getName(), productivity.getId());
        assertFalse(productivity.isOpen());
        assertEquals(26, productivity.getContent().get(0).size());
        assertEquals("30", productivity.getContent().get(0).get("column_3").getTitle());
        assertEquals("Productividad polivalente", productivity.getContent().get(0).get("column_3")
                .getTooltip().get("title_1"));
        assertEquals("0 uds/h", productivity.getContent().get(0).get("column_3")
                .getTooltip().get("subtitle_1"));

        assertEquals(THROUGHPUT.getName(), throughput.getId());
        assertFalse(throughput.isOpen());
        assertFalse(throughput.getContent().isEmpty());

        data.stream().filter(t -> t.getId().equals("throughput"))
                .findFirst()
                .ifPresentOrElse(
                        (value) ->
                                Assertions.assertTrue(value.getContent().stream()
                                        .anyMatch(t -> t.entrySet().stream()
                                                .anyMatch(entry ->
                                                        entry.getKey().equals("column_1")
                                                                && entry.getValue()
                                                                .getTitle()
                                                                .equals(
                                                                        RowName.DEVIATION.getTitle()
                                                                )
                                                )
                                        )
                                ),
                        () -> Assertions.fail("Doesn't exists")
        );

        final Chart chart = projection.getChart();
        final List<ChartData> chartData = chart.getData();

        assertEquals(60, chart.getProcessingTime().getValue());
        assertEquals(5, chartData.size());

        final ChartData chartData1 = chartData.get(0);
        assertEquals(currentTime.plusHours(4).toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData1.getTitle());
        assertEquals(currentTime.plusHours(4).format(DATE_FORMATTER), chartData1.getCpt());
        assertEquals(currentTime.plusHours(2).plusMinutes(35).format(DATE_FORMATTER),
                chartData1.getProjectedEndTime());

        final ChartData chartData2 = chartData.get(1);
        assertEquals(currentTime.plusHours(5).toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData2.getTitle());
        assertEquals(currentTime.plusHours(5).format(DATE_FORMATTER), chartData2.getCpt());
        assertEquals(currentTime.plusHours(3).format(DATE_FORMATTER),
                chartData2.getProjectedEndTime());

        final ChartData chartData3 = chartData.get(2);
        assertEquals(
                currentTime.plusHours(5).plusMinutes(30)
                        .toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData3.getTitle()
        );
        assertEquals(currentTime.plusHours(5).plusMinutes(30).format(DATE_FORMATTER),
                chartData3.getCpt());
        assertEquals(currentTime.plusHours(3).plusMinutes(20).format(DATE_FORMATTER),
                chartData3.getProjectedEndTime());

        final ChartData chartData4 = chartData.get(3);
        assertEquals(currentTime.plusHours(6).toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData4.getTitle()
        );
        assertEquals(currentTime.plusHours(6).format(DATE_FORMATTER), chartData4.getCpt());
        assertEquals(currentTime.plusHours(8).plusMinutes(11).format(DATE_FORMATTER),
                chartData4.getProjectedEndTime());

        final ChartData chartData5 = chartData.get(4);
        assertEquals(currentTime.plusHours(7).toLocalTime().format(HOUR_MINUTES_FORMAT),
                chartData5.getTitle()
        );
        assertEquals(currentTime.plusHours(7).format(DATE_FORMATTER), chartData5.getCpt());
        assertEquals(currentTime.plusDays(1).format(DATE_FORMATTER),
                chartData5.getProjectedEndTime());

        final SimpleTable simpleTable = projection.getSimpleTable2();
        assertEquals(5, simpleTable.getColumns().size());
        assertEquals("Cierre actual", simpleTable.getColumns().get(3).getTitle());
        assertTrue(simpleTable.getColumns().contains(
                new ColumnHeader("column_5", "Cierre simulado"))
        );

        final List<Map<String, String>> simpleTableData = simpleTable.getData();
        assertEquals(5, simpleTableData.size());
        simpleTableData.forEach((dataRow) -> assertTrue(dataRow.containsKey("column_5")));
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
                        .date(utcCurrentTime.plusHours(5))
                        .projectedEndDate(utcCurrentTime.plusHours(3))
                        .simulatedEndDate(utcCurrentTime.plusHours(3))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(5).plusMinutes(30))
                        .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
                        .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(6))
                        .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
                        .simulatedEndDate(utcCurrentTime.plusHours(8).plusMinutes(11))
                        .remainingQuantity(180)
                        .build(),
                ProjectionResult.builder()
                        .date(utcCurrentTime.plusHours(7))
                        .remainingQuantity(100)
                        .build()
        );
    }

    private SimulationRequest createSimulationRequest(final List<Backlog> backlogs,
                                                      final ZonedDateTime currentTime) {
        return SimulationRequest.builder()
                .processName(List.of(PICKING, PACKING))
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .backlog(backlogs.stream()
                        .map(backlog -> new QuantityByDate(
                                backlog.getDate(),
                                backlog.getQuantity()))
                        .collect(toList()))
                .simulations(List.of(new Simulation(PICKING, List.of(new SimulationEntity(
                        HEADCOUNT, List.of(new QuantityByDate(currentTime, 20))
                )))))
                .build();
    }

    private List<Backlog> mockBacklog() {
        final ZonedDateTime currentTime = getCurrentTime();

        return List.of(
                Backlog.builder()
                        .date(currentTime.minusHours(1))
                        .quantity(150)
                        .build(),
                Backlog.builder()
                        .date(currentTime.plusHours(2))
                        .quantity(235)
                        .build(),
                Backlog.builder()
                        .date(currentTime.plusHours(3))
                        .quantity(300)
                        .build()
        );
    }

    private List<Backlog> mockSales() {
        final ZonedDateTime currentTime = getCurrentTime();

        return List.of(
                Backlog.builder()
                        .date(currentTime.plusHours(1))
                        .quantity(350)
                        .build(),
                Backlog.builder()
                        .date(currentTime.plusHours(2))
                        .quantity(235)
                        .build(),
                Backlog.builder()
                        .date(currentTime.plusHours(3))
                        .quantity(200)
                        .build()
        );
    }

    private EntityRequest createRequest(final EntityType entityType,
                                        final ZonedDateTime currentTime,
                                        final List<ProcessingType> processingTypes) {
        return EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(entityType)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .processingType(processingTypes)
                .simulations(List.of(new Simulation(PICKING, List.of(new SimulationEntity(
                        HEADCOUNT, List.of(new QuantityByDate(currentTime, 20)))))))
                .build();
    }

    private ZonedDateTime getCurrentTime() {
        return now(UTC).withMinute(0).withSecond(0).withNano(0);
    }

    private List<Entity> mockHeadcountEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(30)
                        .build()
        );
    }

    private List<Entity> mockProductivityEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(60)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusHours(1))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(30)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(50)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(75)
                        .build()
        );
    }

    private List<Entity> mockThroughputEntities() {
        return new ArrayList<>();
    }
}
