package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.entities.monitor.UnitMeasure;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

@Value
public class HistoricalBacklog {
    private final Map<Integer, UnitMeasure> backlog;

    public static HistoricalBacklog emptyBacklog() {
        return new HistoricalBacklog(emptyMap());
    }

    public UnitMeasure get(Instant date) {
        return backlog.get(DateUtils.minutesFromWeekStart(date));
    }

    public UnitMeasure getOr(Instant date, Supplier<UnitMeasure> supplier) {
        UnitMeasure item = backlog.get(DateUtils.minutesFromWeekStart(date));
        if (item == null) {
            return supplier.get();
        }
        return item;
    }
}
