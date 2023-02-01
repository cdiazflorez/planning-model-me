package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.WORKERS;
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
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, 2, List.of(PERFORMED_PROCESSING)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB, 2, List.of(PERFORMED_PROCESSING)))),
  PICKING(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION,
              5,
              List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              5,
              List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  PACKING(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, 10, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              11,
              List.of(WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  BATCH_SORTER(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, 15, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              17,
              List.of(WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  WALL_IN(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, 20, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              23,
              List.of(WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  PACKING_WALL(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, 25, List.of(WORKERS, ACTIVE_WORKERS)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              29,
              List.of(WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  HU_ASSEMBLY(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, NON_EXISTENT_COLUMN_IN_VERSION, Collections.emptyList()),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              35,
              List.of(WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  SALES_DISPATCH(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, NON_EXISTENT_COLUMN_IN_VERSION, Collections.emptyList()),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB,
              41,
              List.of(WORKERS, ACTIVE_WORKERS, ACTIVE_WORKERS_NS)))),
  GLOBAL(
      SheetVersion.mapping(
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.INITIAL_VERSION, 31, List.of(MAX_CAPACITY)),
          new ForecastProcessName.ColumnByVersion(
              SheetVersion.NON_SYSTEMIC_VERSION_OB, 48, List.of(MAX_CAPACITY))));

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
