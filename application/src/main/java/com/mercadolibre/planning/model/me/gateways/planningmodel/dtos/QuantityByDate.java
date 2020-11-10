package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class QuantityByDate {
    private ZonedDateTime date;
    private int quantity;
}
