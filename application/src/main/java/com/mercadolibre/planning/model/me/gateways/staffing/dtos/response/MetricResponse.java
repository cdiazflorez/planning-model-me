package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class MetricResponse {

  String name;
  Instant dateFrom;
  Instant dateTo;
  List<ProcessResponse> processes;
}
