package com.mercadolibre.planning.model.me.gateways.staffing.dtos.request;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.time.Instant;
import lombok.Value;

@Value
public class MetricRequest {

  ProcessName processName;

  Instant dateFrom;

  Instant dateTo;
}
