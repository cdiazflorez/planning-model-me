package com.mercadolibre.planning.model.me.entities.sharedistribution;

import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

/** Contains projected backlog parameters. */
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
