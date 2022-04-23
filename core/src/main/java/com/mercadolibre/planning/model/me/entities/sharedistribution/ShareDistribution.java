package com.mercadolibre.planning.model.me.entities.sharedistribution;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@Value
public class ShareDistribution {

  String logisticCenterId;

  ZonedDateTime date;

  String processName;

  String area;

  Double quantity;

  String quantityMetricUnit;
}
