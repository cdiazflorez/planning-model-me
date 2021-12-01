package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Remembers the value that some backlog related variables have at some instant.
 */
@Value
@Builder
public class VariablesPhoto {
    /**
     * The instant when the photo is taken.
     */
    private Instant date;
    /**
     * The backlog at the instant.
     */
    private UnitMeasure current;
    /**
     * The backlog at the same instant of the previous week.
     */
    private UnitMeasure historical;
    /**
     * The maximum value the backlog should not break through at the instant.
     */
    private UnitMeasure maxLimit;
    /**
     * The minimum value the backlog should not break through at the instant.
     */
    private UnitMeasure minLimit;
}
