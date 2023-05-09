package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class BacklogStatsByDate {
    Instant date;
    UnitMeasure total;
    UnitMeasure historical;
}
