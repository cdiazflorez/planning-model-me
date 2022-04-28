package com.mercadolibre.planning.model.me.gateways.sharedistribution.dto;

import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

/** Map the parameters of a backlog history record. */
@Builder
@Value
public class DistributionElement {
  long sis;

  String area;

  String warehouseID;

  ZonedDateTime cptTime;
}
