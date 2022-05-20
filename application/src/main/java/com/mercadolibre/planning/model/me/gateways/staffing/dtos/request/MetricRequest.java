package com.mercadolibre.planning.model.me.gateways.staffing.dtos.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import java.time.Instant;
import lombok.Value;

@Value
public class MetricRequest {

  ProcessName processName;

  Instant dateFrom;

  Instant dateTo;
}
