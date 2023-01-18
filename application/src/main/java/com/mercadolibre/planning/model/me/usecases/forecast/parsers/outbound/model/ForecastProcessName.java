package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.TOTAL_WORKERS_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.WORKERS;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
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
  WAVING(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(2, List.of(PERFORMED_PROCESSING)),
          new ForecastProcessName.ColumnByVersion(2, List.of(PERFORMED_PROCESSING)))),
  PICKING(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              5, List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              5, List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS, TOTAL_WORKERS_NS)))),
  PACKING(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(10, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              11, List.of(WORKERS, ACTIVE_WORKERS, TOTAL_WORKERS_NS)))),
  BATCH_SORTER(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(15, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              17, List.of(WORKERS, ACTIVE_WORKERS, TOTAL_WORKERS_NS)))),
  WALL_IN(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(20, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              23, List.of(WORKERS, ACTIVE_WORKERS, TOTAL_WORKERS_NS)))),
  PACKING_WALL(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(25, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              29, List.of(WORKERS, ACTIVE_WORKERS, TOTAL_WORKERS_NS)))),
  GLOBAL(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(31, List.of(MAX_CAPACITY)),
          new ForecastProcessName.ColumnByVersion(36, List.of(MAX_CAPACITY))));

  private static final Map<String, ForecastProcessName> LOOKUP =
      Arrays.stream(values()).collect(toMap(ForecastProcessName::toString, Function.identity()));

  private final Map<SheetVersion, ForecastProcessName.ColumnByVersion> startingColumnByVersion;

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

  public List<ForecastProcessType> getProcessTypes(final SheetVersion version) {
    return this.getValidVersion(version).getProcessTypes();
  }

  public int getStartingColumn(final SheetVersion version) {
    return this.getValidVersion(version).getColumn();
  }

  private ColumnByVersion getValidVersion(final SheetVersion version) {
    return this.getStartingColumnByVersion().get(version);
  }

  @Getter
  @AllArgsConstructor
  private static class ColumnByVersion {
    private int column;
    private List<ForecastProcessType> processTypes;
  }
}
