package com.mercadolibre.planning.model.me.usecases;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
import static java.util.TimeZone.getTimeZone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProjectionTest {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");
    private static final TimeZone TIME_ZONE = getTimeZone("America/Buenos_Aires");

    @InjectMocks
    private GetProjection getProjection;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void testExecute() {
        // Given
        final LocalDateTime time = LocalDateTime.now(TIME_ZONE.toZoneId());

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

        when(planningModelGateway.getEntities(createRequest(HEADCOUNT)))
                .thenReturn(mockHeadcountEntities());
        when(planningModelGateway.getEntities(createRequest(PRODUCTIVITY)))
                .thenReturn(mockProductivityEntities());
        when(planningModelGateway.getEntities(createRequest(THROUGHPUT)))
                .thenReturn(mockThroughputEntities());

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
        assertEquals(time.atZone(TIME_ZONE.toZoneId()).withNano(0),
                headcount.getContent().get(0).get("column_2").getDate().withNano(0));
        assertEquals("Cantidad de reps FCST", headcount.getContent().get(0).get("column_2")
                .getTooltip().get("title_2"));
        assertEquals("10", headcount.getContent().get(0).get("column_2")
                .getTooltip().get("subtitle_2"));
        assertEquals("15", headcount.getContent().get(1).get("column_4").getTitle());

        assertEquals(PRODUCTIVITY.getName(), productivity.getId());
        assertTrue(productivity.isOpen());
        assertEquals(26, productivity.getContent().get(0).size());
        assertEquals("30", productivity.getContent().get(0).get("column_3").getTitle());
        assertEquals("Productividad polivalente", productivity.getContent().get(0).get("column_3")
                .getTooltip().get("title_1"));
        assertEquals("0 uds/h", productivity.getContent().get(0).get("column_3")
                .getTooltip().get("subtitle_1"));

        assertEquals(THROUGHPUT.getName(), throughput.getId());
        assertTrue(throughput.isOpen());
        assertTrue(throughput.getContent().isEmpty());
    }

    private EntityRequest createRequest(final EntityType entityType) {
        return EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(entityType)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(now().withMinute(0).withSecond(0).withNano(0))
                .dateTo(now().withMinute(0).withSecond(0).withNano(0).withMinute(0).plusDays(1))
                .build();
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
