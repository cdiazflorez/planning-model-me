package com.mercadolibre.planning.model.me.services.backlog;

import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo.Group;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.PackingRatio;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RatioServiceTest {

  private static final Double ALLOWED_ERROR = 0.001;

  private static final Instant DATE_FROM = Instant.parse("2022-07-17T00:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2022-07-17T03:00:00Z");

  @InjectMocks
  private RatioService ratioService;

  @Mock
  private BacklogApiGateway gateway;

  @Test
  void testGetPackingRatio() {
    // GIVEN
    mockBacklog(DATE_FROM, DATE_TO);

    // WHEN
    final var ratios = ratioService.getPackingRatio(WAREHOUSE_ID, DATE_FROM, DATE_TO, DATE_FROM, DATE_TO);

    // THEN
    assertNotNull(ratios);
    assertEquals(4, ratios.size());

    // assert ratio values
    assertRatio(ratios.get(DATE_FROM), 1D, 0D);
    assertRatio(ratios.get(DATE_FROM.plus(1, ChronoUnit.HOURS)), 0.5416, 0.4583);
    assertRatio(ratios.get(DATE_FROM.plus(2, ChronoUnit.HOURS)), 0.9900, 0.0099);
    assertRatio(ratios.get(DATE_FROM.plus(3, ChronoUnit.HOURS)), 0.5000, 0.5000);
  }

  private void assertRatio(final PackingRatio ratio,
                           final double expectedPackingToteValue,
                           final double expectedPackingWallValue) {

    assertEquals(expectedPackingToteValue, ratio.getPackingToteRatio(), ALLOWED_ERROR);
    assertEquals(expectedPackingWallValue, ratio.getPackingWallRatio(), ALLOWED_ERROR);
  }

  private void mockBacklog(final Instant dateFrom, final Instant dateTo) {

    final var firstDateFrom = dateFrom.minus(7L, ChronoUnit.DAYS);
    final var firstDateTo = dateTo.minus(7L, ChronoUnit.DAYS);
    when(gateway.getPhotosCached(requestToGroup(firstDateFrom, firstDateTo)))
        .thenReturn(
            buildPhotos(
                firstDateFrom,
                groups(100),
                groups(100),
                groups(200),
                groups(201),
                groups(201)
            )
        );

    when(gateway.getPhotosCached(requestToPack(firstDateFrom, firstDateTo)))
        .thenReturn(
            buildPhotos(
                firstDateFrom,
                packs(100, 100, 100),
                packs(150, 150, 150),
                packs(200, 200, 175),
                packs(250, 250, 205),
                packs(250, 250, 205)
            )
        );

    final var secondDateFrom = dateFrom.minus(14L, ChronoUnit.DAYS);
    final var secondDateTo = dateTo.minus(14L, ChronoUnit.DAYS);
    when(gateway.getPhotosCached(requestToGroup(secondDateFrom, secondDateTo)))
        .thenReturn(
            buildPhotos(
                secondDateFrom,
                groups(100),
                groups(100),
                groups(125),
                groups(126),
                groups(126)
            )
        );

    when(gateway.getPhotosCached(requestToPack(secondDateFrom, secondDateTo)))
        .thenReturn(
            buildPhotos(
                secondDateFrom,
                packs(100, 100, 100),
                packs(125, 125, 200),
                packs(150, 175, 300),
                packs(250, 175, 400),
                packs(250, 175, 400)
            )
        );

    final var thirdDateFrom = dateFrom.minus(21L, ChronoUnit.DAYS);
    final var thirdDateTo = dateTo.minus(21L, ChronoUnit.DAYS);
    when(gateway.getPhotosCached(requestToGroup(thirdDateFrom, thirdDateTo)))
        .thenReturn(
            buildPhotos(
                thirdDateFrom,
                groups(100),
                groups(100),
                groups(175),
                groups(175),
                groups(175)
            )
        );

    when(gateway.getPhotosCached(requestToPack(thirdDateFrom, thirdDateTo)))
        .thenReturn(
            buildPhotos(
                thirdDateFrom,
                packs(100, 100, 100),
                packs(115, 115, 100),
                packs(135, 120, 110),
                packs(135, 120, 110),
                packs(135, 120, 110)
            )
        );
  }

  private BacklogPhotosRequest requestToGroup(final Instant dateFrom, final Instant dateTo) {
    return new BacklogPhotosRequest(
        WAREHOUSE_ID,
        Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
        Set.of(Step.TO_GROUP),
        null,
        null,
        dateFrom,
        dateTo,
        Set.of(STEP, AREA),
        dateFrom,
        dateTo.plus(Duration.ofHours(1))
    );
  }

  private BacklogPhotosRequest requestToPack(final Instant dateFrom, final Instant dateTo) {
    return new BacklogPhotosRequest(
        WAREHOUSE_ID,
        Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
        Set.of(Step.TO_PACK),
        null,
        null,
        dateFrom,
        dateTo,
        Set.of(STEP, AREA),
        dateFrom,
        dateTo.plus(Duration.ofHours(1))
    );
  }

  @SafeVarargs
  private List<Photo> buildPhotos(final Instant dateFrom, final List<Group>... groups) {
    return IntStream.range(0, groups.length)
        .mapToObj(i -> new Photo(dateFrom.plus(i, ChronoUnit.HOURS), groups[i]))
        .collect(Collectors.toList());
  }

  private List<Group> groups(final int batchSorterUnits) {
    return List.of(
        new Group(Map.of(STEP, "TO_GROUP"), 10, batchSorterUnits)
    );
  }

  private List<Group> packs(final int someAreaPackingToteUnits,
                            final int anotherAreaPackingToteUnits,
                            final int packingWallUnits) {
    return List.of(
        new Group(Map.of(STEP, "TO_PACK", AREA, "MZ"), 20, someAreaPackingToteUnits),
        new Group(Map.of(STEP, "TO_PACK", AREA, "HV"), 30, anotherAreaPackingToteUnits),
        new Group(Map.of(STEP, "TO_PACK", AREA, "PW"), 30, packingWallUnits)
    );
  }
}
