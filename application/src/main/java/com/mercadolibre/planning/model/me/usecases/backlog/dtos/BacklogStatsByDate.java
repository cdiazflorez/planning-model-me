package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@RequiredArgsConstructor
public class BacklogStatsByDate {
    Instant date;
    UnitMeasure total;
    UnitMeasure historical;
    UnitMeasure minLimit;
    UnitMeasure maxLimit;
}
