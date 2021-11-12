package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class Backlog {
    private ZonedDateTime date;
    private int quantity;
}
