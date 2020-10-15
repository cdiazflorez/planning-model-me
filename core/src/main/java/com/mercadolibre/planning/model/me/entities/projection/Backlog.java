package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class Backlog {
    private ZonedDateTime date;
    private int quantity;
}
