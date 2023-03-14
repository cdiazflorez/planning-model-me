package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Value;

@Value
public class StaffingRow {
  CellValue<ZonedDateTime> date;
  Map<ForecastStaffingColumnName, CellValue<Integer>> productivities;
  Map<ForecastStaffingColumnName, CellValue<Double>> ratios;

  /**
   * GetInvalidCells.
   * @return stream with errors of cell values.
   **/
  public Stream<CellValue<?>> getInvalidCells() {
    final Stream<CellValue<ZonedDateTime>> dateError = date.isValid() ? Stream.empty() : Stream.of(date);
    return Stream.of(
        dateError,
        productivities.values().stream().filter(c -> !c.isValid()),
        ratios.values().stream().filter(c -> !c.isValid())
    ).flatMap(Function.identity());
  }
}
