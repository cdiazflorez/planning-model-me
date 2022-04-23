package com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@AllArgsConstructor
public class GetShareDistributionInput {

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  String wareHouseId;
}
