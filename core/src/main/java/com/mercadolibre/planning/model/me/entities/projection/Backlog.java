package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class Backlog {
    private ZonedDateTime date;
    private int quantity;
}
