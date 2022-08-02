package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.CARRY_OVER;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.PROCESSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Process;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails.BacklogProvider.BacklogProviderInput;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea.NumberOfUnitsInASubarea;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionHeadcount;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountAtArea;
import com.mercadolibre.planning.model.me.usecases.projection.entities.HeadcountBySubArea;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PickingDetailsBacklogServiceTest {

  private static final String WAVING_AREAS = "";

  private static final String AREA_BL = "BL";

  private static final String AREA_MZ = "MZ";

  private static final String AREA_MZ_1 = "MZ-1";

  private static final String AREA_MZ_2 = "MZ-2";

  private static final Instant DATE_FROM = Instant.parse("2022-07-26T08:00:00Z");

  private static final Instant REQUEST_DATE = Instant.parse("2022-07-26T10:20:30Z");

  private static final Instant DATE_TO = Instant.parse("2022-07-26T12:00:00Z");

  private static final String SOME_DATE_OUT = "2022-07-26T20:00:00Z";

  private static final String ANOTHER_DATE_OUT = "2022-07-26T21:00:00Z";

  private static final String YET_ANOTHER_DATE_OUT = "2022-07-26T22:00:00Z";

  @InjectMocks
  private PickingDetailsBacklogService service;

  @Mock
  private BacklogPhotoApiGateway backlogGateway;

  @Mock
  private ProjectBacklog backlogProjectionService;

  @Mock
  private GetProjectionHeadcount headcountProjectionService;

  private static BacklogProviderInput input(List<MagnitudePhoto> throughput) {
    final var slaFrom = REQUEST_DATE.minus(0, ChronoUnit.HOURS);
    final var slaTo = REQUEST_DATE.plus(24, ChronoUnit.HOURS);

    return new BacklogProviderInput(
        REQUEST_DATE,
        TestUtils.WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        PICKING,
        throughput,
        DATE_FROM,
        DATE_TO,
        slaFrom,
        slaTo,
        TestUtils.USER_ID
    );
  }

  @Test
  void testExecuteOk() {
    // GIVEN
    final List<MagnitudePhoto> throughput = throughput(100, 200, 0, 300, 250);

    mockBacklog();
    mockProjection(throughput, false);
    mockHeadcount();

    // WHEN
    final var results = service.getMonitorBacklog(input(throughput));

    // THEN
    assertEquals(6, results.size());
    assertPastResult(results, "2022-07-26T08:00:00Z", 50, 110);
    assertPastResult(results, "2022-07-26T09:00:00Z", 70, 250);
    assertPastResult(results, "2022-07-26T10:00:00Z", 269, 524);
    assertPastResult(results, "2022-07-26T10:20:30Z", 260, 396);

    final var firstProjectedDate = results.get(Instant.parse("2022-07-26T11:00:00Z"));
    assertNotNull(firstProjectedDate);

    assertEquals(expectedBlResult(), firstProjectedDate.get(0));
    assertEquals(expectedMzResult(), firstProjectedDate.get(1));
  }

  @Test
  void testMissingBacklog() {
    // GIVEN
    final List<MagnitudePhoto> throughput = throughput(100, 200, 0, 300, 250);

    mockMissingBacklog();
    mockProjection(throughput, true);
    mockHeadcount();

    // WHEN
    final var results = service.getMonitorBacklog(input(throughput));

    // THEN
    assertEquals(6, results.size());
    assertPastResult(results, "2022-07-26T08:00:00Z", 50, 110);
    assertPastResult(results, "2022-07-26T10:00:00Z", 269, 524);

    assertMissingBacklog(results, "2022-07-26T09:00:00Z");
    assertMissingBacklog(results, "2022-07-26T10:20:30Z");

    final var firstProjectedDate = results.get(Instant.parse("2022-07-26T11:00:00Z"));
    assertNotNull(firstProjectedDate);

    assertEquals(expectedBlResult(), firstProjectedDate.get(0));
    assertEquals(expectedMzResult(), firstProjectedDate.get(1));
  }

  private NumberOfUnitsInAnArea expectedBlResult() {
    final var subAreas = List.of(new NumberOfUnitsInASubarea(AREA_BL, 5));
    return new NumberOfUnitsInAnArea(AREA_BL, subAreas, 3, 0.15);
  }

  private NumberOfUnitsInAnArea expectedMzResult() {
    final var subAreas = List.of(
        new NumberOfUnitsInASubarea(AREA_MZ_1, 20, 15, 0.75),
        new NumberOfUnitsInASubarea(AREA_MZ_2, 20, 2, 0.10)
    );

    return new NumberOfUnitsInAnArea(AREA_MZ, subAreas, 17, 0.85);
  }

  private void assertPastResult(final Map<Instant, List<NumberOfUnitsInAnArea>> results,
                                final String date,
                                final int bl,
                                final int mz) {

    final var result = results.get(Instant.parse(date));

    assertNotNull(result);
    assertEquals(2, result.size());

    assertEquals(AREA_BL, result.get(0).getArea());
    assertEquals(bl, result.get(0).getUnits());

    assertEquals(AREA_MZ, result.get(1).getArea());
    assertEquals(mz, result.get(1).getUnits());
  }

  private void assertMissingBacklog(final Map<Instant, List<NumberOfUnitsInAnArea>> results,
                                    final String date) {

    final var result = results.get(Instant.parse(date));

    assertNotNull(result);
    assertEquals(1, result.size());

    assertEquals("N/A", result.get(0).getArea());
    assertEquals(0, result.get(0).getUnits());
    assertEquals(0, result.get(0).getSubareas().size());
  }

  private BacklogRequest backlogRequest() {
    final var slaFrom = REQUEST_DATE.minus(0, ChronoUnit.HOURS);
    final var slaTo = REQUEST_DATE.plus(24, ChronoUnit.HOURS);

    return new BacklogRequest(
        TestUtils.WAREHOUSE_ID,
        Set.of(Workflow.FBM_WMS_OUTBOUND),
        Set.of(Process.WAVING, Process.PICKING),
        DATE_FROM,
        DATE_TO,
        null,
        null,
        slaFrom,
        slaTo,
        Set.of(BacklogGrouper.STEP, BacklogGrouper.AREA, BacklogGrouper.DATE_OUT)
    );
  }

  private void mockBacklog() {
    final var waving = List.of(
        new Photo(DATE_FROM, List.of(
            group(30, SOME_DATE_OUT, WAVING_AREAS),
            group(40, ANOTHER_DATE_OUT, WAVING_AREAS)
        )),
        new Photo(DATE_FROM.plus(1L, ChronoUnit.HOURS), List.of(
            group(40, SOME_DATE_OUT, WAVING_AREAS),
            group(50, ANOTHER_DATE_OUT, WAVING_AREAS),
            group(100, YET_ANOTHER_DATE_OUT, WAVING_AREAS)
        )),
        new Photo(DATE_FROM.plus(2L, ChronoUnit.HOURS), List.of(
            group(10, SOME_DATE_OUT, WAVING_AREAS),
            group(80, ANOTHER_DATE_OUT, WAVING_AREAS),
            group(120, YET_ANOTHER_DATE_OUT, WAVING_AREAS)
        )),
        new Photo(REQUEST_DATE, List.of(
            group(70, SOME_DATE_OUT, WAVING_AREAS),
            group(30, ANOTHER_DATE_OUT, WAVING_AREAS),
            group(170, YET_ANOTHER_DATE_OUT, WAVING_AREAS)
        ))
    );

    final var picking = List.of(
        new Photo(DATE_FROM, List.of(
            group(80, SOME_DATE_OUT, AREA_MZ),
            group(30, ANOTHER_DATE_OUT, AREA_MZ),
            group(50, ANOTHER_DATE_OUT, AREA_BL)
        )),
        new Photo(DATE_FROM.plus(30, ChronoUnit.MINUTES), List.of(
            group(80, SOME_DATE_OUT, AREA_MZ)
        )),
        new Photo(DATE_FROM.plus(1L, ChronoUnit.HOURS), List.of(
            group(40, SOME_DATE_OUT, AREA_MZ),
            group(70, SOME_DATE_OUT, AREA_BL),
            group(210, ANOTHER_DATE_OUT, AREA_MZ)
        )),
        new Photo(DATE_FROM.plus(90, ChronoUnit.MINUTES), List.of(
            group(80, SOME_DATE_OUT, AREA_MZ)
        )),
        new Photo(DATE_FROM.plus(2L, ChronoUnit.HOURS), List.of(
            group(122, SOME_DATE_OUT, AREA_MZ),
            group(2, SOME_DATE_OUT, AREA_BL),
            group(156, ANOTHER_DATE_OUT, AREA_MZ),
            group(75, ANOTHER_DATE_OUT, AREA_BL),
            group(246, YET_ANOTHER_DATE_OUT, AREA_MZ),
            group(192, YET_ANOTHER_DATE_OUT, AREA_BL)
        )),
        new Photo(REQUEST_DATE, List.of(
            group(86, SOME_DATE_OUT, AREA_MZ),
            group(63, SOME_DATE_OUT, AREA_BL),
            group(103, ANOTHER_DATE_OUT, AREA_MZ),
            group(59, ANOTHER_DATE_OUT, AREA_BL),
            group(207, YET_ANOTHER_DATE_OUT, AREA_MZ),
            group(138, YET_ANOTHER_DATE_OUT, AREA_BL)
        ))
    );

    Mockito.when(backlogGateway.getBacklogDetails(backlogRequest()))
        .thenReturn(Map.of(Process.WAVING, waving, Process.PICKING, picking));
  }

  private void mockMissingBacklog() {
    final var waving = List.of(
        new Photo(DATE_FROM, List.of(
            group(30, SOME_DATE_OUT, WAVING_AREAS),
            group(40, ANOTHER_DATE_OUT, WAVING_AREAS)
        )),
        new Photo(DATE_FROM.plus(2L, ChronoUnit.HOURS), List.of(
            group(10, SOME_DATE_OUT, WAVING_AREAS),
            group(80, ANOTHER_DATE_OUT, WAVING_AREAS),
            group(120, YET_ANOTHER_DATE_OUT, WAVING_AREAS)
        ))
    );

    final var picking = List.of(
        new Photo(DATE_FROM, List.of(
            group(80, SOME_DATE_OUT, AREA_MZ),
            group(30, ANOTHER_DATE_OUT, AREA_MZ),
            group(50, ANOTHER_DATE_OUT, AREA_BL)
        )),
        new Photo(DATE_FROM.plus(30, ChronoUnit.MINUTES), List.of(
            group(80, SOME_DATE_OUT, AREA_MZ)
        )),
        new Photo(DATE_FROM.plus(90, ChronoUnit.MINUTES), List.of(
            group(80, SOME_DATE_OUT, AREA_MZ)
        )),
        new Photo(DATE_FROM.plus(2L, ChronoUnit.HOURS), List.of(
            group(122, SOME_DATE_OUT, AREA_MZ),
            group(2, SOME_DATE_OUT, AREA_BL),
            group(156, ANOTHER_DATE_OUT, AREA_MZ),
            group(75, ANOTHER_DATE_OUT, AREA_BL),
            group(246, YET_ANOTHER_DATE_OUT, AREA_MZ),
            group(192, YET_ANOTHER_DATE_OUT, AREA_BL)
        ))
    );

    Mockito.when(backlogGateway.getBacklogDetails(backlogRequest()))
        .thenReturn(Map.of(Process.WAVING, waving, Process.PICKING, picking));
  }

  private void mockProjection(final List<MagnitudePhoto> throughput, final boolean isMissingBacklog) {
    final var backlog = isMissingBacklog
        ? Collections.emptySet()
        : Set.of(
        new BacklogQuantityAtSla(WAVING, Instant.parse(SOME_DATE_OUT), 70),
        new BacklogQuantityAtSla(WAVING, Instant.parse(ANOTHER_DATE_OUT), 30),
        new BacklogQuantityAtSla(WAVING, Instant.parse(YET_ANOTHER_DATE_OUT), 170),
        new BacklogQuantityAtSla(PICKING, Instant.parse(SOME_DATE_OUT), 149),
        new BacklogQuantityAtSla(PICKING, Instant.parse(ANOTHER_DATE_OUT), 162),
        new BacklogQuantityAtSla(PICKING, Instant.parse(YET_ANOTHER_DATE_OUT), 345)
    );

    final var firstDate = REQUEST_DATE;
    final var secondDate = DATE_TO.minus(1, ChronoUnit.HOURS);
    final var thirdDate = DATE_TO;

    final var waving = Stream.of(
        new ProjectedBacklogForAnAreaAndOperatingHour(firstDate, WAVING, WAVING_AREAS, PROCESSED, 100L),
        new ProjectedBacklogForAnAreaAndOperatingHour(firstDate, WAVING, WAVING_AREAS, CARRY_OVER, 100L),
        new ProjectedBacklogForAnAreaAndOperatingHour(secondDate, WAVING, WAVING_AREAS, PROCESSED, 100L),
        new ProjectedBacklogForAnAreaAndOperatingHour(secondDate, WAVING, WAVING_AREAS, CARRY_OVER, 100L),
        new ProjectedBacklogForAnAreaAndOperatingHour(thirdDate, WAVING, WAVING_AREAS, PROCESSED, 100L),
        new ProjectedBacklogForAnAreaAndOperatingHour(thirdDate, WAVING, WAVING_AREAS, CARRY_OVER, 100L)
    );

    final var pickingFirst = pickingProjection(firstDate, 50, 100, 150, 0, 10, 20);
    final var pickingSecond = pickingProjection(secondDate, 150, 30, 20, 20, 5, 20);
    final var pickingThird = pickingProjection(thirdDate, 0, 20, 50, 0, 10, 20);

    Mockito.when(
        backlogProjectionService.projectBacklogInAreas(
            Mockito.eq(REQUEST_DATE),
            Mockito.eq(DATE_TO),
            Mockito.eq(TestUtils.WAREHOUSE_ID),
            Mockito.eq(Workflow.FBM_WMS_OUTBOUND),
            Mockito.eq(List.of(WAVING, PICKING)),
            Mockito.argThat(arg -> backlog.equals(new HashSet<>(arg))),
            Mockito.eq(throughput)
        )
    ).thenReturn(
        Stream.of(
                waving, pickingFirst, pickingSecond, pickingThird
            )
            .flatMap(Function.identity())
            .collect(Collectors.toList())
    );
  }

  private void mockHeadcount() {
    final var firstDate = DATE_TO.minus(1, ChronoUnit.HOURS);
    final var secondDate = DATE_TO;

    final var pickingProcessedBacklog = Map.of(
        firstDate, Set.of(
            new NumberOfUnitsInAnArea(AREA_MZ,
                List.of(
                    new NumberOfUnitsInASubarea(AREA_MZ_1, 150),
                    new NumberOfUnitsInASubarea(AREA_MZ_2, 20)
                )
            ),
            new NumberOfUnitsInAnArea(AREA_BL, List.of(new NumberOfUnitsInASubarea(AREA_BL, 30)))
        ),
        secondDate, Set.of(
            new NumberOfUnitsInAnArea(AREA_MZ,
                List.of(
                    new NumberOfUnitsInASubarea(AREA_MZ_1, 0),
                    new NumberOfUnitsInASubarea(AREA_MZ_2, 50)
                )
            ),
            new NumberOfUnitsInAnArea(AREA_BL, List.of(new NumberOfUnitsInASubarea(AREA_BL, 20)))
        )
    );

    Mockito.when(headcountProjectionService.getProjectionHeadcount(
                Mockito.eq(TestUtils.WAREHOUSE_ID),
                Mockito.argThat(arg -> arg.entrySet()
                    .stream()
                    .allMatch(entry -> pickingProcessedBacklog.get(entry.getKey()).equals(new HashSet<>(entry.getValue()))))
            )
        )
        .thenReturn(
            Map.of(
                firstDate, List.of(
                    new HeadcountAtArea(AREA_MZ, 17, 0.85, List.of(
                        new HeadcountBySubArea(AREA_MZ_1, 15, 0.75),
                        new HeadcountBySubArea(AREA_MZ_2, 2, 0.1)
                    )),
                    new HeadcountAtArea(AREA_BL, 3, 0.15, Collections.emptyList())
                ),
                secondDate, List.of(
                    new HeadcountAtArea(AREA_MZ, 5, 0.5, List.of(
                        new HeadcountBySubArea(AREA_MZ_1, 0, 0.0),
                        new HeadcountBySubArea(AREA_MZ_2, 5, 0.5)
                    )),
                    new HeadcountAtArea(AREA_BL, 5, 0.5, Collections.emptyList())
                )
            )
        );

  }

  private Stream<ProjectedBacklogForAnAreaAndOperatingHour> pickingProjection(final Instant date, long... values) {
    return Stream.of(
        new ProjectedBacklogForAnAreaAndOperatingHour(date, PICKING, AREA_MZ_1, PROCESSED, values[0]),
        new ProjectedBacklogForAnAreaAndOperatingHour(date, PICKING, AREA_BL, PROCESSED, values[1]),
        new ProjectedBacklogForAnAreaAndOperatingHour(date, PICKING, AREA_MZ_2, PROCESSED, values[2]),
        new ProjectedBacklogForAnAreaAndOperatingHour(date, PICKING, AREA_MZ_1, CARRY_OVER, values[3]),
        new ProjectedBacklogForAnAreaAndOperatingHour(date, PICKING, AREA_BL, CARRY_OVER, values[4]),
        new ProjectedBacklogForAnAreaAndOperatingHour(date, PICKING, AREA_MZ_2, CARRY_OVER, values[5])
    );
  }

  private List<MagnitudePhoto> throughput(int... values) {
    final long hours = values.length;
    final var dateFrom = DATE_FROM.atZone(ZoneOffset.UTC);

    return LongStream.range(0, hours)
        .mapToObj(i -> MagnitudePhoto.builder()
            .date(dateFrom.plusHours(i))
            .value(values[(int) i])
            .processName(PICKING)
            .build()
        )
        .collect(Collectors.toList());
  }

  private Photo.Group group(final int total, final String dateOut, final String area) {
    return new Photo.Group(
        Map.of(BacklogGrouper.DATE_OUT, dateOut, BacklogGrouper.AREA, area),
        total
    );
  }

}
