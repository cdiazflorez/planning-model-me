package com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos;

import java.time.ZonedDateTime;
import lombok.Value;

/** Contains query parameters of ShareDistribution. */
@Value
public class GetShareDistributionInput {

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  String wareHouseId;
}
