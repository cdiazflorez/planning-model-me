package com.mercadolibre.planning.model.me.services.backlog;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.RatioInputGroup;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.RatioWeightsSource;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.UnitsPerDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PackingRatioCalculatorTest {

  private static final double ALLOWED_DEVIATION = 0.001;

  private static final Instant DATE_1 = Instant.parse("2022-07-19T00:00:00Z");

  private static final Instant DATE_2 = Instant.parse("2022-07-19T01:00:00Z");

  private static final Instant DATE_3 = Instant.parse("2022-07-19T02:00:00Z");

  @Test
  void testCalculate() {
    // GIVEN
    final Map<Instant, RatioInputGroup> input = Map.of(
        DATE_1, new RatioInputGroup(unitsWithValues(DATE_1, 100, 50, 30), unitsWithValues(DATE_1, 0, 0, 0)),
        DATE_2, new RatioInputGroup(unitsWithValues(DATE_2, 100, 75, 25), unitsWithValues(DATE_2, 100, 25, 75)),
        DATE_3, new RatioInputGroup(unitsWithValues(DATE_3, 100, 100, 0), unitsWithValues(DATE_3, 1, 1, 0))
    );

    final var weight = getWeights();

    // WHEN
    final var result = PackingRatioCalculator.calculate(input, weight);

    // THEN
    assertEquals(3, result.size());

    final var firstResult = result.get(DATE_1);
    assertEquals(1D, firstResult.getPackingToteRatio(), ALLOWED_DEVIATION);
    assertEquals(0D, firstResult.getPackingWallRatio(), ALLOWED_DEVIATION);

    final var secondResult = result.get(DATE_2);
    assertEquals(0.5375, secondResult.getPackingToteRatio(), ALLOWED_DEVIATION);
    assertEquals(0.4625, secondResult.getPackingWallRatio(), ALLOWED_DEVIATION);

    final var thirdResult = result.get(DATE_3);
    assertEquals(0.9900990099, thirdResult.getPackingToteRatio(), ALLOWED_DEVIATION);
    assertEquals(0.009900990099, thirdResult.getPackingWallRatio(), ALLOWED_DEVIATION);
  }

  private RatioWeightsSource getWeights() {
    final var weight = Mockito.mock(RatioWeightsSource.class);
    when(weight.at(DATE_1.minus(1, DAYS))).thenReturn(0.65);
    when(weight.at(DATE_1.minus(2, DAYS))).thenReturn(0.25);
    when(weight.at(DATE_1.minus(3, DAYS))).thenReturn(0.10);

    when(weight.at(DATE_2.minus(1, DAYS))).thenReturn(0.65);
    when(weight.at(DATE_2.minus(2, DAYS))).thenReturn(0.25);
    when(weight.at(DATE_2.minus(3, DAYS))).thenReturn(0.10);

    when(weight.at(DATE_3.minus(1, DAYS))).thenReturn(0.65);
    when(weight.at(DATE_3.minus(2, DAYS))).thenReturn(0.25);
    when(weight.at(DATE_3.minus(3, DAYS))).thenReturn(0.10);

    return weight;
  }

  private List<UnitsPerDate> unitsWithValues(final Instant startingDate, final Integer... units) {
    return IntStream.rangeClosed(1, units.length)
        .mapToObj(i -> new UnitsPerDate(startingDate.minus(i, DAYS), units[i - 1]))
        .collect(Collectors.toList());
  }

}
