package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.time.Instant;
import java.util.List;

/** Remembers the backlog at some instant break down by area. */
@Value
public class DetailedBacklogPhoto {
    /**
     * The instant in question.
     */
    private Instant date;
    /**
     * Desired backlog at the instant in question.
     */
    private UnitMeasure target;
    /**
     * Total backlog at the instant in question.
     */
    private UnitMeasure total;
    /**
     * The backlog at the instant in question break down by area.
     */
    private List<AreaBacklogDetail> areas;
}
