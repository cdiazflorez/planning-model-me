package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class BacklogStatsByDate {
    Instant date;
    UnitMeasure total;
    UnitMeasure historical;
    UnitMeasure minLimit;
    UnitMeasure maxLimit;
}
