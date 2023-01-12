package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.TOTAL_WORKERS_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.WORKERS;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastProcessName {
    WAVING(List.of(PERFORMED_PROCESSING), 2, 2),
    PICKING(List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS), 5, 5),
    PACKING(List.of(WORKERS, ACTIVE_WORKERS), 10, 11),
    BATCH_SORTER(List.of(WORKERS, ACTIVE_WORKERS), 15, 17),
    WALL_IN(List.of(WORKERS, ACTIVE_WORKERS), 20, 23),
    PACKING_WALL(List.of(WORKERS, ACTIVE_WORKERS), 25, 29),
    GLOBAL(List.of(MAX_CAPACITY), 31, 36);

  private static final Map<String, ForecastProcessName> LOOKUP =
      Arrays.stream(values()).collect(toMap(ForecastProcessName::toString, Function.identity()));
  private final List<ForecastProcessType> processTypes;
  private final int startingColumn;

  private final int startingColumnNewVersion;

  public static Stream<ForecastProcessName> stream() {
    return Stream.of(ForecastProcessName.values());
  }

  @JsonCreator
  public static ForecastProcessName from(final String status) {
    return LOOKUP.get(status.toLowerCase());
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
