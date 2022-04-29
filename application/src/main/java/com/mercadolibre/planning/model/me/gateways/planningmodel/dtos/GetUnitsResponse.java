package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import lombok.Value;

/** Response of units distribution. */
@Value
public class GetUnitsResponse {

  long id;

  String logisticCenterId;

  ZonedDateTime date;

  String processName;

  String area;

  Double quantity;

  MetricUnit quantityMetricUnit;
}
