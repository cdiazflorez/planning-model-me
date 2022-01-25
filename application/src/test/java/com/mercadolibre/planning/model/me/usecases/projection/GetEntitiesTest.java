package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
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
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.*;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetEntitiesTest {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:00");

    @InjectMocks
    private GetEntities getEntities;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void testExecute() {
        //GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
                .thenReturn(new LogisticCenterConfiguration(getDefault()));

        when(planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .entityTypes(List.of(HEADCOUNT, THROUGHPUT, PRODUCTIVITY))
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusDays(1))
                .processName(List.of(PICKING, PACKING, PACKING_WALL))
                .entityFilters(Map.of(
                        HEADCOUNT, Map.of(
                                PROCESSING_TYPE.toJson(),
                                List.of(ACTIVE_WORKERS.getName())
                        ),
                        PRODUCTIVITY, Map.of(
                                ABILITY_LEVEL.toJson(),
                                List.of("1","2")
                        ))
                )
                .build())
        ).thenReturn(
                Map.of(
                        HEADCOUNT, mockHeadcountEntities(utcCurrentTime),
                        PRODUCTIVITY, mockProductivityEntities(utcCurrentTime),
                        THROUGHPUT, new ArrayList<>()
                )
        );


        //WHEN
        final ComplexTable response = getEntities.execute(GetProjectionInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .build());

        //THEN
        assertComplexTable(response);
    }

    private List<MagnitudePhoto> mockHeadcountEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                MagnitudePhoto.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(30)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime.plusHours(3))
                        .processName(PACKING_WALL)
                        .source(Source.FORECAST)
                        .value(79)
                        .build(),
                MagnitudePhoto.builder()
                        .date(utcCurrentTime.plusDays(3))
                        .processName(PACKING_WALL)
                        .source(Source.FORECAST)
                        .value(32)
                        .build()
        );
    }

    private List<MagnitudePhoto> mockProductivityEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Productivity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(60)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(1))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(30)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(50)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(75)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(50)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(1))
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(40)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(65)
                        .abilityLevel(2)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PACKING)
                        .source(Source.FORECAST)
                        .value(98)
                        .abilityLevel(1)
                        .build(),
                Productivity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PACKING_WALL)
                        .source(Source.FORECAST)
                        .value(14)
                        .abilityLevel(3)
                        .build()
        );
    }

    private void assertComplexTable(final ComplexTable complexTable) {
        final ZonedDateTime currentTime = getCurrentUtcDate()
                .withZoneSameInstant(getDefault().toZoneId());
        final List<ColumnHeader> columns = complexTable.getColumns();
        final List<Data> data = complexTable.getData();

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
        assertEquals("Productividad polivalente", productivity.getContent()
                .get(0).get("column_3")
                .getTooltip().get("title_1"));
        assertEquals("20 uds/h", productivity.getContent().get(0).get("column_3")
                .getTooltip().get("subtitle_1"));

        assertEquals(THROUGHPUT.getName(), throughput.getId());
        assertFalse(throughput.isOpen());
        assertTrue(throughput.getContent().isEmpty());
    }
}
