package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.INITIAL_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.SECOND_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.mapping;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.NON_EXISTENT_COLUMN_IN_VERSION;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastProcessName {
  WAVING(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, 2, List.of(PERFORMED_PROCESSING)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION, 2, List.of(PERFORMED_PROCESSING)))),
  PICKING(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION,
              5,
              List.of(REMAINING_PROCESSING, ACTIVE_WORKERS, EFFECTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              5,
              List.of(REMAINING_PROCESSING, ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  PACKING(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, 10, List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              11,
              List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  BATCH_SORTER(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, 15, List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              17,
              List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  WALL_IN(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, 20, List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              23,
              List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  PACKING_WALL(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, 25, List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              29,
              List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  HU_ASSEMBLY(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, NON_EXISTENT_COLUMN_IN_VERSION, Collections.emptyList()),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              35,
              List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  SALES_DISPATCH(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, NON_EXISTENT_COLUMN_IN_VERSION, Collections.emptyList()),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION,
              41,
              List.of(ACTIVE_WORKERS, EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS)))),
  GLOBAL(
      mapping(
          new ForecastProcessName.ColumnByVersion(
              INITIAL_VERSION, 31, List.of(MAX_CAPACITY)),
          new ForecastProcessName.ColumnByVersion(
              SECOND_VERSION, 48, List.of(MAX_CAPACITY))));

  private static final Map<String, ForecastProcessName> LOOKUP =
      Arrays.stream(values()).collect(toMap(ForecastProcessName::toString, Function.identity()));

  private final Map<SheetVersion, ForecastProcessName.ColumnByVersion> startingColumnByVersion;

  public static Stream<ForecastProcessName> stream() {
    return Stream.of(ForecastProcessName.values());
  }

  @JsonCreator
  public static ForecastProcessName from(final String status) {
    return LOOKUP.get(status.toLowerCase(Locale.ROOT));
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
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
    private SheetVersion sheetVersion;
    private int column;
    private List<ForecastProcessType> processTypes;
  }
}
