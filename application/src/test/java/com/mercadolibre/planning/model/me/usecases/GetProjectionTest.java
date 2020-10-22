package com.mercadolibre.planning.model.me.usecases;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProjectionTest {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");
    private static final TimeZone TIME_ZONE = getDefault();

    @InjectMocks
    private GetProjection getProjection;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private GetBacklog getBacklog;

    @Test
    public void testExecute() {
        // Given
        final ZonedDateTime time = getCurrentTime();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(planningModelGateway.getEntities(createRequest(HEADCOUNT)))
                .thenReturn(mockHeadcountEntities());
        when(planningModelGateway.getEntities(createRequest(PRODUCTIVITY)))
                .thenReturn(mockProductivityEntities());
        when(planningModelGateway.getEntities(createRequest(THROUGHPUT)))
                .thenReturn(mockThroughputEntities());

        final List<Backlog> mockedBacklog = mockBacklog();
        when(getBacklog.execute(new GetBacklogInputDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID)))
                .thenReturn(mockedBacklog);
        when(planningModelGateway.runProjection(createProjectionRequest(mockedBacklog)))
                .thenReturn(mockProjections());

        // When
        final Projection projection = getProjection.execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build()
        );

        // Then
        assertEquals("Proyecciones", projection.getTitle());

        final List<ColumnHeader> columns = projection.getComplexTable1().getColumns();
        final List<Data> data = projection.getComplexTable1().getData();

        IntStream.range(0, 24).forEach(index -> {
            assertEquals("column_" + (index + 2), columns.get(index + 1).getId());
            assertEquals(time.plusHours(index).format(HOUR_FORMAT),
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
        assertEquals(time, headcount.getContent().get(0).get("column_2").getDate()
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
        assertTrue(throughput.getContent().isEmpty());

        final Chart chart = projection.getChart();
        final List<ChartData> chartData = chart.getData();
        final ChartData chartData1 = chartData.get(0);
        final ChartData chartData2 = chartData.get(1);
        final ChartData chartData3 = chartData.get(2);


        assertEquals(60, chart.getProcessingTime().getValue());
        assertEquals(3, chartData.size());

        assertEquals(String.valueOf(getCurrentTime().plusHours(4).getHour()),
                chartData1.getTitle());
        assertEquals(String.valueOf(getCurrentTime().plusHours(4)), chartData1.getCpt());
        assertEquals(getCurrentTime().plusHours(2).plusMinutes(30),
                chartData1.getProjectedEndTime());

        assertEquals(String.valueOf(getCurrentTime().plusHours(5).getHour()),
                chartData2.getTitle());
        assertEquals(String.valueOf(getCurrentTime().plusHours(5)), chartData2.getCpt());
        assertEquals(getCurrentTime().plusHours(3), chartData2.getProjectedEndTime());

        assertEquals(String.valueOf(getCurrentTime().plusHours(6).getHour()),
                chartData3.getTitle());
        assertEquals(String.valueOf(getCurrentTime().plusHours(6)), chartData3.getCpt());
        assertEquals(getCurrentTime().plusHours(8).plusMinutes(10),
                chartData3.getProjectedEndTime());
    }

    private List<ProjectionResult> mockProjections() {
        return List.of(
                ProjectionResult.builder()
                        .date(getCurrentTime().plusHours(4))
                        .projectedEndDate(getCurrentTime().plusHours(2).plusMinutes(30))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(getCurrentTime().plusHours(5))
                        .projectedEndDate(getCurrentTime().plusHours(3))
                        .remainingQuantity(0)
                        .build(),
                ProjectionResult.builder()
                        .date(getCurrentTime().plusHours(6))
                        .projectedEndDate(getCurrentTime().plusHours(8).plusMinutes(10))
                        .remainingQuantity(180)
                        .build()
        );
    }

    private ProjectionRequest createProjectionRequest(final List<Backlog> backlogs) {
        return ProjectionRequest.builder()
                .processName(List.of(PICKING, PACKING))
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(getCurrentTime())
                .dateTo(getCurrentTime().plusHours(25))
                .type(ProjectionType.CPT)
                .backlog(backlogs)
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

    private EntityRequest createRequest(final EntityType entityType) {
        final ZonedDateTime currentTime = getCurrentTime();

        return EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(entityType)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .build();
    }

    private ZonedDateTime getCurrentTime() {
        return now().withMinute(0).withSecond(0).withNano(0);
    }

    private List<Entity> mockHeadcountEntities() {
        return List.of(
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()).plusHours(2))
                        .processName(PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()).plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(30)
                        .build()
        );
    }

    private List<Entity> mockProductivityEntities() {
        return List.of(
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(60)
                        .build(),
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()).plusHours(1))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(30)
                        .build(),
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()).plusHours(2))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(50)
                        .build(),
                Entity.builder()
                        .date(now(TIME_ZONE.toZoneId()).plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(75)
                        .build()
        );
    }

    private List<Entity> mockThroughputEntities() {
        return emptyList();
    }
}
