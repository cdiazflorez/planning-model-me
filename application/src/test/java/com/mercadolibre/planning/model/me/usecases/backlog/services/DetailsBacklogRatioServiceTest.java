package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DetailsBacklogRatioServiceTest {
  private static final Instant DATE_FROM = Instant.parse("2022-07-26T08:00:00Z");

  private static final Instant REQUEST_DATE = Instant.parse("2022-07-26T10:20:30Z");

  private static final Instant DATE_TO = Instant.parse("2022-07-26T12:00:00Z");

  @InjectMocks
  private DetailsBacklogRatioService detailsBacklogRatioService;

  @Mock
  private BacklogPhotoApiGateway backlogGateway;

  @Mock
  private ProjectionGateway projectionGateway;

  @Mock
  private RatioService ratioService;

  private GetBacklogMonitorDetails.BacklogProvider.BacklogProviderInput input(final boolean hasWall) {
    final var slaFrom = REQUEST_DATE.minus(0, ChronoUnit.HOURS);
    final var slaTo = REQUEST_DATE.plus(24, ChronoUnit.HOURS);

    return new GetBacklogMonitorDetails.BacklogProvider.BacklogProviderInput(
        REQUEST_DATE,
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        WAVING,
        emptyList(),
        DATE_FROM,
        DATE_TO,
        slaFrom,
        slaTo,
        USER_ID,
        hasWall
    );
  }

  @Test
  void testExecuteOk() {
    // GIVEN
    mockBacklog();
    mockProjection(650);
    when(ratioService.getPackingRatio(WAREHOUSE_ID, DATE_FROM, DATE_TO)).thenReturn(emptyMap());

    // WHEN
    final var results = detailsBacklogRatioService.getMonitorBacklog(input(true));

    // THEN
    assertNotNull(results);
    assertEquals(6, results.size());

    assertResult(results, "2022-07-26T08:00:00Z", 700);
    assertResult(results, "2022-07-26T09:00:00Z", 850);
    assertResult(results, "2022-07-26T10:00:00Z", 600);
    assertResult(results, "2022-07-26T10:20:30Z", 650);
    assertResult(results, "2022-07-26T11:00:00Z", 150);
    assertResult(results, "2022-07-26T12:00:00Z", 330);
  }

  @Test
  void testExecuteOkHasWallFalse() {
    // GIVEN
    mockBacklog();
    mockProjection(650);

    // WHEN
    final var results = detailsBacklogRatioService.getMonitorBacklog(input(false));

    // THEN
    assertNotNull(results);
    assertEquals(6, results.size());

    assertResult(results, "2022-07-26T08:00:00Z", 700);
    assertResult(results, "2022-07-26T09:00:00Z", 850);
    assertResult(results, "2022-07-26T10:00:00Z", 600);
    assertResult(results, "2022-07-26T10:20:30Z", 650);
    assertResult(results, "2022-07-26T11:00:00Z", 150);
    assertResult(results, "2022-07-26T12:00:00Z", 330);
  }

  @Test
  void testMissingBacklog() {
    // GIVEN
    mockMissingBacklog();
    mockProjection(0);
    when(ratioService.getPackingRatio(WAREHOUSE_ID, DATE_FROM, DATE_TO)).thenReturn(emptyMap());

    // WHEN
    final var results = detailsBacklogRatioService.getMonitorBacklog(input(true));

    // THEN
    assertNotNull(results);
    assertEquals(6, results.size());

    assertResult(results, "2022-07-26T08:00:00Z", 700);
    assertResult(results, "2022-07-26T09:00:00Z", 0);
    assertResult(results, "2022-07-26T10:00:00Z", 600);
    assertResult(results, "2022-07-26T10:20:30Z", 0);
    assertResult(results, "2022-07-26T11:00:00Z", 150);
    assertResult(results, "2022-07-26T12:00:00Z", 330);
  }

  @Test
  void testFailToGetBacklogProjections() {
    // GIVEN
    mockBacklog();
    mockFailedProjections();
    when(ratioService.getPackingRatio(WAREHOUSE_ID, DATE_FROM, DATE_TO)).thenReturn(emptyMap());

    // WHEN
    final var results = detailsBacklogRatioService.getMonitorBacklog(input(true));

    // THEN
    assertNotNull(results);
    assertEquals(4, results.size());

    assertResult(results, "2022-07-26T08:00:00Z", 700);
    assertResult(results, "2022-07-26T09:00:00Z", 850);
    assertResult(results, "2022-07-26T10:00:00Z", 600);
    assertResult(results, "2022-07-26T10:20:30Z", 650);
  }

  private void assertResult(final Map<Instant, List<NumberOfUnitsInAnArea>> results, final String date, final int quantity) {
    final var result = results.get(Instant.parse(date));

    assertNotNull(result, String.format("Could not find register for %s", date));
    assertEquals(1, result.size());
    assertEquals(quantity, result.get(0).getUnits());
  }

  private BacklogRequest backlogRequest() {
    final var slaFrom = REQUEST_DATE.minus(0, ChronoUnit.HOURS);
    final var slaTo = REQUEST_DATE.plus(24, ChronoUnit.HOURS);

    return new BacklogRequest(
        WAREHOUSE_ID,
        Set.of(FBM_WMS_OUTBOUND),
        Set.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL),
        DATE_FROM,
        REQUEST_DATE,
        null,
        null,
        slaFrom,
        slaTo,
        Set.of(BacklogGrouper.STEP, BacklogGrouper.AREA)
    );
  }

  private void mockBacklog() {
    final var firstDate = DATE_FROM;
    final var secondDate = DATE_FROM.plus(1, ChronoUnit.HOURS);
    final var thirdDate = DATE_FROM.plus(2, ChronoUnit.HOURS);
    final var fourthDate = REQUEST_DATE;

    when(
        backlogGateway.getTotalBacklogPerProcessAndInstantDate(backlogRequest())
    ).thenReturn(
        Map.of(
            WAVING, List.of(
                new BacklogPhoto(firstDate, 700),
                new BacklogPhoto(firstDate.plus(30, ChronoUnit.MINUTES), 1000),
                new BacklogPhoto(secondDate, 850),
                new BacklogPhoto(secondDate.plus(30, ChronoUnit.MINUTES), 1500),
                new BacklogPhoto(thirdDate, 600),
                new BacklogPhoto(fourthDate, 650)
            )
        )
    );
  }

  private void mockMissingBacklog() {
    final var firstDate = DATE_FROM;
    final var secondDate = DATE_FROM.plus(1, ChronoUnit.HOURS);
    final var thirdDate = DATE_FROM.plus(2, ChronoUnit.HOURS);

    when(
        backlogGateway.getTotalBacklogPerProcessAndInstantDate(backlogRequest())
    ).thenReturn(
        Map.of(
            WAVING, List.of(
                new BacklogPhoto(firstDate, 700),
                new BacklogPhoto(firstDate.plus(30, ChronoUnit.MINUTES), 1000),
                new BacklogPhoto(secondDate.plus(30, ChronoUnit.MINUTES), 1500),
                new BacklogPhoto(thirdDate, 600)
            )
        )
    );
  }

  private void mockProjection(final int currentBacklog) {
    final var dateFrom = REQUEST_DATE.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
    final var dateTo = DATE_TO.atZone(ZoneOffset.UTC);

    when(
        projectionGateway.getBacklogProjection(
            BacklogProjectionRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .processName(List.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL))
                .currentBacklog(List.of(
                    new CurrentBacklog(WAVING, currentBacklog),
                    new CurrentBacklog(PICKING, 0),
                    new CurrentBacklog(PACKING, 0),
                    new CurrentBacklog(BATCH_SORTER, 0),
                    new CurrentBacklog(WALL_IN, 0),
                    new CurrentBacklog(PACKING_WALL, 0)
                ))
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .applyDeviation(true)
                .packingWallRatios(emptyMap())
                .build()
        )
    ).thenReturn(
        List.of(
            new BacklogProjectionResponse(WAVING, List.of(
                new ProjectionValue(dateFrom.plusHours(1L), 150),
                new ProjectionValue(dateFrom.plusHours(2L), 330)
            )),
            new BacklogProjectionResponse(PICKING, List.of(
                new ProjectionValue(dateFrom.plusHours(1L), 330),
                new ProjectionValue(dateFrom.plusHours(2L), 140)
            )),
            new BacklogProjectionResponse(PACKING, List.of(
                new ProjectionValue(dateFrom.plusHours(1L), 180),
                new ProjectionValue(dateFrom.plusHours(2L), 440)
            ))
        )
    );
  }

  private void mockFailedProjections() {
    when(
        projectionGateway.getBacklogProjection(Mockito.any(BacklogProjectionRequest.class))
    ).thenThrow(RuntimeException.class);
  }
}
