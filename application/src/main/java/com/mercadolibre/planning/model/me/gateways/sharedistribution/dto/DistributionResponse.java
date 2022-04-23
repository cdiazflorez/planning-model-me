package com.mercadolibre.planning.model.me.gateways.sharedistribution.dto;

import lombok.Builder;
import lombok.Value;


import java.time.ZonedDateTime;

@Builder
@Value
public class DistributionResponse {
  long sis;

  String area;

  String warehouseID;

  ZonedDateTime cptTime;
}
