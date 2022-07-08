package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastProcessType {
  PERFORMED_PROCESSING(0, UNITS),
  REMAINING_PROCESSING(-2, MINUTES),
  WORKERS(1, MetricUnit.WORKERS),
  ACTIVE_WORKERS(2, MetricUnit.WORKERS),
  HEADCOUNT_PRODUCTIVITY(3, UNITS_PER_HOUR),
  MAX_CAPACITY(0, UNITS_PER_HOUR),
  BACKLOG_LOWER_LIMIT(20, MINUTES),
  BACKLOG_UPPER_LIMIT(21, MINUTES);

  private static final Map<String, ForecastProcessType> LOOKUP = Arrays.stream(values()).collect(
      toMap(ForecastProcessType::toString, Function.identity())
  );

  /**
   * Indicates how many columns to shift between the starting column (as indicated by ForecastProcessName.startingColumn) and the column
   * where the process type can be found.
   */
  private final int columnOrder;

  private final MetricUnit metricUnit;

  @JsonCreator
  public static ForecastProcessType from(String status) {
    return LOOKUP.get(status);
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.US);
  }
}
