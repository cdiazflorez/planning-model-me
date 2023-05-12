package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class QuantityByDate {
    private ZonedDateTime date;
    private int quantity;
}
