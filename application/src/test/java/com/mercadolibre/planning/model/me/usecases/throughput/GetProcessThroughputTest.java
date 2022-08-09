package com.mercadolibre.planning.model.me.usecases.throughput;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetProcessThroughputTest {

  @InjectMocks
  private GetProcessThroughput getProcessThroughput;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Test
  void testGetWavingThroughput() {
    // GIVEN
    final GetThroughputInput input = buildThroughputInput(WAVING);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(PICKING, PACKING, PACKING_WALL))
    )).thenReturn(
        Map.of(THROUGHPUT, Stream.of(
                pickingTrajectory(),
                packingTrajectory(),
                packingWallTrajectory())
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
    final GetThroughputInput input = buildThroughputInput(PICKING);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(PICKING))
    )).thenReturn(
        Map.of(THROUGHPUT, pickingTrajectory().collect(Collectors.toList()))
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
    final GetThroughputInput input = buildThroughputInput(BATCH_SORTER);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(BATCH_SORTER))
    )).thenReturn(
        Map.of(THROUGHPUT, batchSorterTrajectory().collect(Collectors.toList()))
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
    final GetThroughputInput input = buildThroughputInput(WALL_IN);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(WALL_IN))
    )).thenReturn(
        Map.of(THROUGHPUT, wallInTrajectory().collect(Collectors.toList()))
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
    final GetThroughputInput input = buildThroughputInput(PACKING);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(PACKING, PACKING_WALL))
    )).thenReturn(
        Map.of(THROUGHPUT,
               Stream.concat(
                   packingTrajectory(), packingWallTrajectory()
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
    final GetThroughputInput input = buildThroughputInput(PACKING_WALL);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(PACKING_WALL))
    )).thenReturn(
        Map.of(THROUGHPUT, packingWallTrajectory().collect(Collectors.toList()))
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
    final var input = buildThroughputInput(GLOBAL);

    when(planningModelGateway.searchTrajectories(
        buildSearchTrajectoriesRequest(of(GLOBAL))
    )).thenReturn(
        Map.of(THROUGHPUT, globalTrajectory().collect(Collectors.toList()))
    );

    // WHEN
    var results = getProcessThroughput.execute(input);

    // THEN
    assertNotNull(results);
    assertThroughputs(results.get(GLOBAL), 300, 400, 500);
  }

  private GetThroughputInput buildThroughputInput(ProcessName process) {
    final ZonedDateTime dateFrom = TestUtils.A_DATE;

    return GetThroughputInput.builder()
        .warehouseId(TestUtils.WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .processes(of(process))
        .dateFrom(dateFrom)
        .dateTo(dateFrom.plusHours(2L))
        .source(FORECAST)
        .build();
  }

  private SearchTrajectoriesRequest buildSearchTrajectoriesRequest(List<ProcessName> processes) {
    final ZonedDateTime dateFrom = TestUtils.A_DATE;

    return SearchTrajectoriesRequest.builder()
        .warehouseId(TestUtils.WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .processName(processes)
        .entityTypes(of(THROUGHPUT))
        .dateFrom(dateFrom)
        .dateTo(dateFrom.plusHours(2L))
        .source(SIMULATION)
        .build();
  }

  private Stream<MagnitudePhoto> pickingTrajectory() {
    return mockTrajectory(PICKING, 10, 75, 40);
  }

  private Stream<MagnitudePhoto> batchSorterTrajectory() {
    return mockTrajectory(BATCH_SORTER, 25, 30, 45);
  }

  private Stream<MagnitudePhoto> wallInTrajectory() {
    return mockTrajectory(WALL_IN, 50, 75, 95);
  }

  private Stream<MagnitudePhoto> packingTrajectory() {
    return mockTrajectory(PACKING, 10, 30, 40);
  }

  private Stream<MagnitudePhoto> packingWallTrajectory() {
    return mockTrajectory(PACKING_WALL, 5, 40, 40);
  }

  private Stream<MagnitudePhoto> globalTrajectory() {
    return mockTrajectory(GLOBAL, 300, 400, 500);
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

  private Stream<MagnitudePhoto> mockTrajectory(ProcessName process,
                                                Integer firstVal,
                                                Integer secondVal,
                                                Integer thirdVal) {

    final var dateFrom = TestUtils.A_DATE;
    return Stream.of(
        MagnitudePhoto.builder()
            .date(dateFrom)
            .processName(process)
            .value(firstVal)
            .build(),
        MagnitudePhoto.builder()
            .date(dateFrom.plusHours(1L))
            .processName(process)
            .value(secondVal)
            .build(),
        MagnitudePhoto.builder()
            .date(dateFrom.plusHours(2L))
            .processName(process)
            .value(thirdVal)
            .build());
  }
}
