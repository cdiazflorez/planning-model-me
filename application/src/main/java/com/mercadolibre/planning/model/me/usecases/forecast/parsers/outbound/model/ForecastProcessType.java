package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastProcessType {
  PERFORMED_PROCESSING(SheetVersion.mapping(0, 0), UNITS),
  REMAINING_PROCESSING(SheetVersion.mapping(-2, -2), MINUTES),
  WORKERS(SheetVersion.mapping(1, 1), MetricUnit.WORKERS),
  ACTIVE_WORKERS(SheetVersion.mapping(2, 2), MetricUnit.WORKERS),
  ACTIVE_WORKERS_NS(SheetVersion.mapping(-99, 3), MetricUnit.WORKERS),
  HEADCOUNT_PRODUCTIVITY(SheetVersion.mapping(3, 4), UNITS_PER_HOUR),
  MAX_CAPACITY(SheetVersion.mapping(0, 0), UNITS_PER_HOUR),
  BACKLOG_LOWER_LIMIT(SheetVersion.mapping(20, 20), MINUTES),
  BACKLOG_UPPER_LIMIT(SheetVersion.mapping(21, 21), MINUTES);

  private static final Map<String, ForecastProcessType> LOOKUP =
      Arrays.stream(values()).collect(toMap(ForecastProcessType::toString, Function.identity()));

  /**
   * Indicates how many columns to shift between the starting column (as indicated by
   * ForecastProcessName.startingColumn) and the column where the process type can be found.
   */
  private final Map<SheetVersion, Integer> columnOrderByVersion;

  private final MetricUnit metricUnit;

  @JsonCreator
  public static ForecastProcessType from(String status) {
    return LOOKUP.get(status);
  }

  public int getColumnOrder(final SheetVersion version) {
    return this.columnOrderByVersion.get(version);
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.US);
  }
}
