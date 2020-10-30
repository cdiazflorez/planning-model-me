package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class QuantityByDate {
    private ZonedDateTime date;
    private int quantity;
}
