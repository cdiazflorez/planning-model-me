package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;
import java.util.List;
import lombok.Value;

@Value
public class BacklogLimit {

  ProcessPath processPath;

  ForecastProcessName processName;

  ForecastProcessType type;

  MetricUnit quantityMetricUnit;

  List<BacklogLimitData> data;
}
