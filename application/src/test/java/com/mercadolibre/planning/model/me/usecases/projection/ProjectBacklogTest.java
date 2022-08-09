package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.CARRY_OVER;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.PROCESSED;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.ORDER_GROUP_TYPE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectBacklogTest {

  @Mock
  protected PlanningModelGateway planningModel;

  @InjectMocks
  private ProjectBacklog projectBacklog;

  @Mock
  private ProjectionGateway projectionGateway;

  @Mock
  private EntityGateway entityGateway;

  @Mock
  private RatioService ratioService;

  @Test
  public void testExecuteOutbound() {
    // GIVEN
    final ZonedDateTime firstDate = getNextHour(A_DATE);

    when(ratioService.getPackingRatio(WAREHOUSE_ID, A_DATE.toInstant(), A_DATE.plusHours(25).toInstant())).thenReturn(emptyMap());

    when(planningModel.getBacklogProjection(
        BacklogProjectionRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .processName(List.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL))
            .dateFrom(A_DATE)
            .dateTo(firstDate.plusHours(25))
            .currentBacklog(List.of(
                new CurrentBacklog(WAVING, 0),
                new CurrentBacklog(PICKING, 2232),
                new CurrentBacklog(PACKING, 1442)))
            .applyDeviation(true)
            .packingWallRatios(emptyMap())
            .build()))
        .thenReturn(List.of(
            new BacklogProjectionResponse(WAVING, List.of(
                new ProjectionValue(firstDate, 100),
                new ProjectionValue(firstDate.plusHours(1), 200)
            )),
            new BacklogProjectionResponse(PICKING, List.of(
                new ProjectionValue(firstDate, 120),
                new ProjectionValue(firstDate.plusHours(1), 220)
            )),
            new BacklogProjectionResponse(PACKING, List.of(
                new ProjectionValue(firstDate, 130),
                new ProjectionValue(firstDate.plusHours(1), 230)
            ))
        ));

    final BacklogProjectionInput input = BacklogProjectionInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .processName(List.of(WAVING, PICKING, PACKING))
        .userId(1L)
        .dateFrom(A_DATE)
        .dateTo(A_DATE.plusHours(25))
        .groupType(ORDER_GROUP_TYPE)
        .backlogs(List.of(
            new CurrentBacklog(WAVING, 0),
            new CurrentBacklog(PICKING, 2232),
            new CurrentBacklog(PACKING, 1442)
        ))
        .hasWall(true)
        .build();

    // WHEN
    final List<BacklogProjectionResponse> projections = projectBacklog.execute(input);

    // THEN
    assertNotNull(projections);

    assertEquals(3, projections.size());

    BacklogProjectionResponse wavingProjections = projections.get(0);
    assertEquals(WAVING, wavingProjections.getProcessName());
    assertEquals(firstDate, wavingProjections.getValues().get(0).getDate());
    assertEquals(100, wavingProjections.getValues().get(0).getQuantity());
    assertEquals(200, wavingProjections.getValues().get(1).getQuantity());
  }

  @Test
  public void testExecuteInbound() {
    // GIVEN
    final ZonedDateTime firstDate = getNextHour(A_DATE);

    when(planningModel.getBacklogProjection(
        BacklogProjectionRequest.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_INBOUND)
            .processName(List.of(CHECK_IN, PUT_AWAY))
            .dateFrom(A_DATE)
            .dateTo(firstDate.plusHours(25))
            .currentBacklog(List.of(
                new CurrentBacklog(CHECK_IN, 100),
                new CurrentBacklog(PUT_AWAY, 120)))
            .applyDeviation(true)
            .packingWallRatios(emptyMap())
            .build()))
        .thenReturn(List.of(
            new BacklogProjectionResponse(CHECK_IN, List.of(
                new ProjectionValue(firstDate, 100),
                new ProjectionValue(firstDate.plusHours(1), 200)
            )),
            new BacklogProjectionResponse(PUT_AWAY, List.of(
                new ProjectionValue(firstDate, 120),
                new ProjectionValue(firstDate.plusHours(1), 220)
            ))
        ));

    final BacklogProjectionInput input = BacklogProjectionInput.builder()
        .workflow(FBM_WMS_INBOUND)
        .warehouseId(WAREHOUSE_ID)
        .processName(List.of(CHECK_IN, PUT_AWAY))
        .userId(1L)
        .dateFrom(A_DATE)
        .dateTo(A_DATE.plusHours(25))
        .backlogs(List.of(
                      new CurrentBacklog(CHECK_IN, 100),
                      new CurrentBacklog(PUT_AWAY, 120)
                  )
        )
        .build();

    // WHEN
    final List<BacklogProjectionResponse> projections = projectBacklog.execute(input);

    // THEN
    assertNotNull(projections);

    assertEquals(2, projections.size());

    BacklogProjectionResponse checkinProjections = projections.get(0);
    assertEquals(CHECK_IN, checkinProjections.getProcessName());
    assertEquals(firstDate, checkinProjections.getValues().get(0).getDate());
    assertEquals(100, checkinProjections.getValues().get(0).getQuantity());
    assertEquals(200, checkinProjections.getValues().get(1).getQuantity());
  }

  @Test
  void testProjectBacklogByArea() {
    // GIVEN
    final var dateFrom = Instant.parse("2022-05-05T18:00:00Z");
    final var zonedDateFrom = ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
    final var dateTo = Instant.parse("2022-05-05T20:00:00Z");
    final var zonedDateTo = ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC);

    final var processes = List.of(WAVING, PICKING);

    final var backlog = List.of(
        new BacklogQuantityAtSla(WAVING, Instant.parse("2022-05-06T11:00:00Z"), 100),
        new BacklogQuantityAtSla(WAVING, Instant.parse("2022-05-06T12:00:00Z"), 2000),
        new BacklogQuantityAtSla(PICKING, Instant.parse("2022-05-06T13:00:00Z"), 150),
        new BacklogQuantityAtSla(PICKING, Instant.parse("2022-05-06T14:00:00Z"), 70)
    );

    final var throughput = List.of(
        MagnitudePhoto.builder()
            .date(zonedDateFrom)
            .workflow(FBM_WMS_OUTBOUND)
            .processName(WAVING)
            .value(300)
            .build(),
        MagnitudePhoto.builder()
            .date(zonedDateTo)
            .workflow(FBM_WMS_OUTBOUND)
            .processName(PICKING)
            .value(250)
            .build()
    );

    final var planned = List.of(
        PlanningDistributionResponse.builder()
            .dateIn(zonedDateFrom)
            .dateOut(zonedDateTo)
            .metricUnit(UNITS)
            .total(100)
            .build(),
        PlanningDistributionResponse.builder()
            .dateIn(zonedDateFrom.plusHours(1L))
            .dateOut(zonedDateTo)
            .metricUnit(UNITS)
            .total(300)
            .build()
    );

    when(planningModel.getPlanningDistribution(any(PlanningDistributionRequest.class))).thenReturn(planned);

    when(entityGateway.getShareDistribution(
        any(GetShareDistributionInput.class),
        eq(FBM_WMS_OUTBOUND)
    )).thenReturn(List.of(
        new GetUnitsResponse(0, WAREHOUSE_ID, zonedDateFrom, "picking", "BL-0", 0.15, UNITS),
        new GetUnitsResponse(0, WAREHOUSE_ID, zonedDateFrom, "picking", "MZ-1", 0.85, UNITS),
        new GetUnitsResponse(0, WAREHOUSE_ID, zonedDateTo, "picking", "MZ-1", 0.5, UNITS),
        new GetUnitsResponse(0, WAREHOUSE_ID, zonedDateTo, "picking", "MZ-2", 0.5, UNITS)
    ));

    final var mappedShare = List.of(
        new BacklogAreaDistribution(PICKING, dateFrom, "BL-0", 0.15),
        new BacklogAreaDistribution(PICKING, dateFrom, "MZ-1", 0.85),
        new BacklogAreaDistribution(PICKING, dateTo, "MZ-1", 0.5),
        new BacklogAreaDistribution(PICKING, dateTo, "MZ-2", 0.5)
    );

    when(projectionGateway.projectBacklogInAreas(
        dateFrom,
        dateTo.plusSeconds(60L * 60L),
        FBM_WMS_OUTBOUND,
        processes,
        backlog,
        planned,
        throughput,
        mappedShare
    )).thenReturn(
        List.of(
            new ProjectedBacklogForAnAreaAndOperatingHour(dateFrom, PICKING, "MZ-1", PROCESSED, 10L),
            new ProjectedBacklogForAnAreaAndOperatingHour(dateFrom, PICKING, "MZ-2", PROCESSED, 50L),
            new ProjectedBacklogForAnAreaAndOperatingHour(dateFrom, PICKING, "MZ-1", CARRY_OVER, 30L),
            new ProjectedBacklogForAnAreaAndOperatingHour(dateFrom, PICKING, "MZ-2", CARRY_OVER, 22L)
        )
    );

    // WHEN
    final var result =
        projectBacklog.projectBacklogInAreas(dateFrom, dateTo, WAREHOUSE_ID, FBM_WMS_OUTBOUND, processes, backlog, throughput);

    // THEN
    assertNotNull(result);
    assertEquals(4, result.size());

    final var firstResult = result.get(0);
    assertEquals(dateFrom, firstResult.getOperatingHour());
    assertEquals(PICKING, firstResult.getProcess());
    assertEquals("MZ-1", firstResult.getArea());
    assertEquals(PROCESSED, firstResult.getStatus());
    assertEquals(10L, firstResult.getQuantity());

    final var lastResult = result.get(3);
    assertEquals(dateFrom, lastResult.getOperatingHour());
    assertEquals(PICKING, lastResult.getProcess());
    assertEquals("MZ-2", lastResult.getArea());
    assertEquals(CARRY_OVER, lastResult.getStatus());
    assertEquals(22L, lastResult.getQuantity());
  }
}