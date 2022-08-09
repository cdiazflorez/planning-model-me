package com.mercadolibre.planning.model.me.services.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConstantStepRatioWeightTest {

  private static final Double ALLOWED_ERROR = 0.001;

  static Stream<Arguments> parameters() {
    return Stream.of(
        arguments("2022-07-16T23:59:59Z", 3D),
        arguments("2022-07-16T00:00:00Z", 3D),
        arguments("2022-07-13T10:00:00Z", 3D),
        arguments("2022-07-10T00:00:00Z", 3D),
        arguments("2022-07-09T23:59:59Z", 2D),
        arguments("2022-07-06T00:00:00Z", 2D),
        arguments("2022-07-03T00:00:00Z", 2D),
        arguments("2022-07-02T23:59:59Z", 1D),
        arguments("2022-06-29T00:00:00Z", 1D),
        arguments("2022-06-26T00:00:00Z", 1D),
        arguments("2022-06-25T23:59:59Z", 0D),
        arguments("2022-06-25T00:00:00Z", 0D)
    );
  }

  @ParameterizedTest
  @MethodSource("parameters")
  void testWeightValues(final String targetDate, final double expectedValue) {
    // GIVEN
    final var dateFrom = Instant.parse("2022-07-17T00:00:00Z");
    final var weighs = new RatioService.ConstantStepRatioWeight(dateFrom, 3);

    final var date = Instant.parse(targetDate);

    // WHEN
    final double value = weighs.at(date);

    // THEN
    assertEquals(expectedValue, value, ALLOWED_ERROR);
  }
}
