package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.CURRENT_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.NON_SYSTEMIC_VERSION_OB;
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
  PERFORMED_PROCESSING(Map.of(CURRENT_VERSION, 0), UNITS),
  REMAINING_PROCESSING(Map.of(CURRENT_VERSION, -2), MINUTES),
  WORKERS(Map.of(CURRENT_VERSION, 1), MetricUnit.WORKERS),
  ACTIVE_WORKERS(Map.of(CURRENT_VERSION, 2), MetricUnit.WORKERS),
  TOTAL_WORKERS_NS(Map.of(NON_SYSTEMIC_VERSION_OB, 3), MetricUnit.WORKERS),
  HEADCOUNT_PRODUCTIVITY(Map.of(CURRENT_VERSION, 3, NON_SYSTEMIC_VERSION_OB, 4), UNITS_PER_HOUR),
  MAX_CAPACITY(Map.of(CURRENT_VERSION, 0), UNITS_PER_HOUR),
  BACKLOG_LOWER_LIMIT(Map.of(CURRENT_VERSION, 20), MINUTES),
  BACKLOG_UPPER_LIMIT(Map.of(CURRENT_VERSION, 21), MINUTES);

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
    return this.columnOrderByVersion.containsKey(version)
        ? this.columnOrderByVersion.get(version)
        : this.columnOrderByVersion.get(CURRENT_VERSION);
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.US);
  }
}
