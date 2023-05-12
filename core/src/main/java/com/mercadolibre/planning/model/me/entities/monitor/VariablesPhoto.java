package com.mercadolibre.planning.model.me.entities.monitor;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

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
}
