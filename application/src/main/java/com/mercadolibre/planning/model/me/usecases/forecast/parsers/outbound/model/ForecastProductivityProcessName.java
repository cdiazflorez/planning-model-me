package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastProductivityProcessName {
  PICKING(SheetVersion.mapping(2, 2)),
  BATCH_SORTER(SheetVersion.mapping(3, 3)),
  WALL_IN(SheetVersion.mapping(4, 4)),
  PACKING(SheetVersion.mapping(5, 5)),
  PACKING_WALL(SheetVersion.mapping(6, 6)),
  HU_ASSEMBLY(SheetVersion.mapping(-99, 7)),
  SALES_DISPATCH(SheetVersion.mapping(-99, 8));

  private final Map<SheetVersion, Integer> columnIndexByVersion;

  public static Stream<ForecastProductivityProcessName> stream() {
    return Stream.of(ForecastProductivityProcessName.values());
  }

  public int getColumnIndex(final SheetVersion version) {
    return this.columnIndexByVersion.get(version);
  }

  public String getName() {
    return name().toLowerCase(Locale.ENGLISH);
  }
}
