package com.mercadolibre.planning.model.me.services.backlog;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.PackingWallRatiosGateway;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RatioServiceTest {

  private static final List<Instant> DATES_LIST = List.of(
      Instant.parse("2022-07-17T00:00:00Z"),
      Instant.parse("2022-07-17T01:00:00Z"),
      Instant.parse("2022-07-17T02:00:00Z"),
      Instant.parse("2022-07-17T03:00:00Z"));

  @InjectMocks
  private RatioService ratioService;

  @Mock
  private PackingWallRatiosGateway gateway;

  @Test
  void testGetPackingRatio() {
    // GIVEN
    final HashMap<Instant, PackingRatio> ratioByHour = new HashMap<>();
    ratioByHour.put(DATES_LIST.get(0), new PackingRatio(0.625, 0.375));
    ratioByHour.put(DATES_LIST.get(1), new PackingRatio(0.100, 0.900));
    ratioByHour.put(DATES_LIST.get(2), new PackingRatio(0.200, 0.800));
    ratioByHour.put(DATES_LIST.get(3), new PackingRatio(0.750, 0.250));

    when(gateway.getPackingWallRatios(WAREHOUSE_ID, DATES_LIST.get(0), DATES_LIST.get(3)))
        .thenReturn(ratioByHour);

    // WHEN
    final var ratios = ratioService.getPackingRatio(WAREHOUSE_ID, DATES_LIST.get(0), DATES_LIST.get(3));
    assertEquals(4, ratios.size());
    assertTrue(ratios.containsKey(DATES_LIST.get(0)));
    assertTrue(ratios.containsKey(DATES_LIST.get(1)));
    assertTrue(ratios.containsKey(DATES_LIST.get(2)));
    assertTrue(ratios.containsKey(DATES_LIST.get(3)));

  }
}
