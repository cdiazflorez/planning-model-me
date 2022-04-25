package com.mercadolibre.planning.model.me.gateways.sharedistribution.dto;

import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

/** Contains the backlog history response parameters. */
@Builder
@Value
public class DistributionResponse {
  long sis;

  String area;

  String warehouseID;

  ZonedDateTime cptTime;
}
