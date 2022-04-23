package com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetShareDistributionInput {

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  String wareHouseId;
}
