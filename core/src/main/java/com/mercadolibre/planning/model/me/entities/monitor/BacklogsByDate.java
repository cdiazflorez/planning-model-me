package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class BacklogsByDate {
    private Instant date;
    private UnitMeasure current;
    private UnitMeasure historical;
    private UnitMeasure maxLimit;
    private UnitMeasure minLimit;
}
