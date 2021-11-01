package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class Backlog {
    private ZonedDateTime date;
    private String status;
    private int quantity;

    public Backlog (final ZonedDateTime date, final int quantity) {
        this.date = date;
        this.quantity = quantity;
    }
}
