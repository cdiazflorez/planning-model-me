package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class BacklogsByDate {
    private ZonedDateTime date;
    private UnitMeasure current;
    private UnitMeasure historical;
    private UnitMeasure maxLimit;
    private UnitMeasure minLimit;
}
