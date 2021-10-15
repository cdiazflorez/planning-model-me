package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class BacklogStatsByDate {
    ZonedDateTime date;
    UnitMeasure total;
    UnitMeasure historical;
    UnitMeasure minLimit;
    UnitMeasure maxLimit;
}
