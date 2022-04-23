package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

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
