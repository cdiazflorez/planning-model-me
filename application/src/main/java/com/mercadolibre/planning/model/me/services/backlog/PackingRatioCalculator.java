package com.mercadolibre.planning.model.me.services.backlog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;

/**
 * Packing Ratio Calculator.
 */
public final class PackingRatioCalculator {

  private static double DEFAULT_RATIO = 0.5;

  private PackingRatioCalculator() {
  }

  /**
   * Maps a list of RatioInputGroup to a list of PackingRatioPerDate by calculating the weighted average of
   * the RatioInputsGroup's UnitsPerDate.
   *
   * <p>Implementation: input and weights parameters are mutually dependent, the weights source should be able to return valid values
   * for all instants in inputs key-set.
   * 
   * @param inputs  backlog groups.
   * @param weights weights by dates.
   * @return ratio by packing and consolidation.
   */
  public static Map<Instant, PackingRatio> calculate(final Map<Instant, RatioInputGroup> inputs, final RatioWeightsSource weights) {
    return inputs.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> getRatioForDate(entry.getValue(), weights)
        ));
  }

  private static PackingRatio getRatioForDate(final RatioInputGroup input, final RatioWeightsSource weights) {
    final var packingWallUnitsByDate = input.getPackingWallUnits()
        .stream()
        .collect(Collectors.toMap(UnitsPerDate::getDate, UnitsPerDate::getQuantity));

    final var packingToteUnitsByDate = input.getPackingToteUnits()
        .stream()
        .collect(Collectors.toMap(UnitsPerDate::getDate, UnitsPerDate::getQuantity));

    final var packingTotePartialRatio = calculatePartialRatio(weights, packingToteUnitsByDate, packingWallUnitsByDate);
    final var packingWallPartialRatio = calculatePartialRatio(weights, packingWallUnitsByDate, packingToteUnitsByDate);

    final var assignedRatio = packingTotePartialRatio + packingWallPartialRatio;

    // TODO: check default value
    final var packingToteFinalRatio = assignedRatio == 0 ? DEFAULT_RATIO : calculateFinalRatio(packingTotePartialRatio, assignedRatio);
    final var packingWallFinalRatio = assignedRatio == 0 ? DEFAULT_RATIO : calculateFinalRatio(packingWallPartialRatio, assignedRatio);

    return new PackingRatio(packingToteFinalRatio, packingWallFinalRatio);
  }

  private static double calculatePartialRatio(final RatioWeightsSource ratioWeight,
                                              final Map<Instant, Integer> targetByDate,
                                              final Map<Instant, Integer> otherByDate) {

    return Stream.of(
            targetByDate.keySet().stream(),
            otherByDate.keySet().stream()
        )
        .flatMap(Function.identity())
        .distinct()
        .mapToDouble(date -> calculateDateRate(date, ratioWeight, targetByDate, otherByDate))
        .sum();
  }

  private static double calculateDateRate(final Instant date,
                                          final RatioWeightsSource ratioWeight,
                                          final Map<Instant, Integer> targetUnits,
                                          final Map<Instant, Integer> otherUnits) {

    final Integer targetQuantity = targetUnits.getOrDefault(date, 0);
    final Integer otherQuantity = otherUnits.getOrDefault(date, 0);
    final Double weight = ratioWeight.at(date);

    if (targetQuantity + otherQuantity == 0) {
      return 0;
    } else {
      return weight * ((double) targetQuantity / (targetQuantity + otherQuantity));
    }
  }

  private static double calculateFinalRatio(final double partialRatio, final double totalAssignedRatio) {
    return partialRatio + ((partialRatio / totalAssignedRatio) * (1 - totalAssignedRatio));
  }

  /**
   * Provides the weight for each item in RatioInputGroup.
   */
  interface RatioWeightsSource {
    Double at(Instant date);
  }

  /**
   * Groups the UnitsPerDate that will be used to calculate de ratio for a specific date.
   */
  @Value
  static class RatioInputGroup {
    List<UnitsPerDate> packingToteUnits;

    List<UnitsPerDate> packingWallUnits;
  }

  @Value
  static class UnitsPerDate {
    Instant date;

    Integer quantity;
  }

  @Value
  static class PackingRatio {
    Double packingToteRatio;

    Double packingWallRatio;
  }
}
