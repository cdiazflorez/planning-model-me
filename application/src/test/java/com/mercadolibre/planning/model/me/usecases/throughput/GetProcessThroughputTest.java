package com.mercadolibre.planning.model.me.usecases.throughput;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProcessThroughputTest {

    @InjectMocks
    private GetProcessThroughput getProcessThroughput;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Spy
    private ThroughputResultMapper mapper;

    @Test
    void testGetWavingThroughput() {
        // GIVEN
        final GetThroughputInput input = getMockInput(ProcessName.WAVING);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(PICKING, PACKING, PACKING_WALL))
        )).thenReturn(
                Map.of(THROUGHPUT, Stream.of(
                                pickingEntities(),
                                packingEntities(),
                                packingWallEntities())
                        .flatMap(e -> e)
                        .collect(Collectors.toList())
                )
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(WAVING), 10, 70, 40);
    }

    @Test
    void testGetPickingThroughput() {
        // GIVEN
        final GetThroughputInput input = getMockInput(PICKING);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(PICKING))
        )).thenReturn(
                Map.of(THROUGHPUT, pickingEntities().collect(Collectors.toList()))
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(PICKING), 10, 75, 40);
    }

    @Test
    void testGetBatchSorterThroughput() {
        // GIVEN
        final GetThroughputInput input = getMockInput(ProcessName.BATCH_SORTER);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(BATCH_SORTER))
        )).thenReturn(
                Map.of(THROUGHPUT, batchSorterEntities().collect(Collectors.toList()))
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(BATCH_SORTER), 25, 30, 45);
    }

    @Test
    void testGetWallInThroughput() {
        // GIVEN
        final GetThroughputInput input = getMockInput(ProcessName.WALL_IN);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(WALL_IN))
        )).thenReturn(
                Map.of(THROUGHPUT, wallInEntities().collect(Collectors.toList()))
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(WALL_IN), 50, 75, 95);
    }

    @Test
    void testGetPackingThroughput() {
        // GIVEN
        final GetThroughputInput input = getMockInput(PACKING);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(PACKING, PACKING_WALL))
        )).thenReturn(
                Map.of(THROUGHPUT,
                        Stream.concat(
                                packingEntities(), packingWallEntities()
                        ).collect(Collectors.toList()))
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(PACKING), 15, 70, 80);
    }

    @Test
    void testGetPackingWallThroughput() {
        // GIVEN
        final GetThroughputInput input = getMockInput(PACKING_WALL);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(PACKING_WALL))
        )).thenReturn(
                Map.of(THROUGHPUT, packingWallEntities().collect(Collectors.toList()))
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(PACKING_WALL), 5, 40, 40);
    }

    @Test
    void testGetGlobalThroughput() {
        // GIVEN
        final var input = getMockInput(ProcessName.GLOBAL);

        when(planningModelGateway.searchEntities(
                getSearchEntitiesRequest(of(GLOBAL))
        )).thenReturn(
                Map.of(THROUGHPUT, globalEntities().collect(Collectors.toList()))
        );

        // WHEN
        var results = getProcessThroughput.execute(input);

        // THEN
        assertNotNull(results);
        assertThroughputs(results.get(GLOBAL), 300, 400, 500);
    }

    private GetThroughputInput getMockInput(ProcessName process) {
        final ZonedDateTime dateFrom = TestUtils.A_DATE;

        return GetThroughputInput.builder()
                .warehouseId(TestUtils.WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processes(List.of(process))
                .dateFrom(dateFrom)
                .dateTo(dateFrom.plusHours(2L))
                .source(FORECAST)
                .build();
    }

    private SearchEntitiesRequest getSearchEntitiesRequest(List<ProcessName> processes) {
        final ZonedDateTime dateFrom = TestUtils.A_DATE;

        return SearchEntitiesRequest.builder()
                .warehouseId(TestUtils.WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processName(processes)
                .entityTypes(of(THROUGHPUT))
                .dateFrom(dateFrom)
                .dateTo(dateFrom.plusHours(2L))
                .source(SIMULATION)
                .build();
    }

    private Stream<Entity> pickingEntities() {
        return mockEntities(PICKING, 10, 75, 40);
    }

    private Stream<Entity> batchSorterEntities() {
        return mockEntities(BATCH_SORTER, 25, 30, 45);
    }

    private Stream<Entity> wallInEntities() {
        return mockEntities(WALL_IN, 50, 75, 95);
    }

    private Stream<Entity> packingEntities() {
        return mockEntities(PACKING, 10, 30, 40);
    }

    private Stream<Entity> packingWallEntities() {
        return mockEntities(PACKING_WALL, 5, 40, 40);
    }

    private Stream<Entity> globalEntities() {
        return mockEntities(GLOBAL, 300, 400, 500);
    }

    private void assertThroughputs(
            Map<ZonedDateTime, Integer> throughputs,
            Integer firstVal,
            Integer secondVal,
            Integer thirdVal) {

        assertNotNull(throughputs);

        final ZonedDateTime dateFrom = TestUtils.A_DATE;
        assertEquals(3, throughputs.size());
        assertEquals(firstVal, throughputs.get(dateFrom));
        assertEquals(secondVal, throughputs.get(dateFrom.plusHours(1L)));
        assertEquals(thirdVal, throughputs.get(dateFrom.plusHours(2L)));
    }

    private Stream<Entity> mockEntities(ProcessName process,
                                        Integer firstVal,
                                        Integer secondVal,
                                        Integer thirdVal) {

        final var dateFrom = TestUtils.A_DATE;
        return Stream.of(
                Entity.builder()
                        .date(dateFrom)
                        .processName(process)
                        .value(firstVal)
                        .build(),
                Entity.builder()
                        .date(dateFrom.plusHours(1L))
                        .processName(process)
                        .value(secondVal)
                        .build(),
                Entity.builder()
                        .date(dateFrom.plusHours(2L))
                        .processName(process)
                        .value(thirdVal)
                        .build());
    }
}
