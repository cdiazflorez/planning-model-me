package com.mercadolibre.planning.model.me.entities.projection;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Knows for each flow step, the length of the queue of units waiting to be processed.
 * Currently, only the pending units (waiting to be waved) are considered.
 */
@Data
@AllArgsConstructor
public class Backlog {
    /**
     * When the photo was taken.
     */
    private ZonedDateTime date;

    String status;

    /**
     * Quantity of units whose status is {@link #status}.
     */
    private int quantity;

    public Backlog(final ZonedDateTime date, final int quantity) {
        this.date = date;
        this.quantity = quantity;
    }
}
