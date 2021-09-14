package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Value
public class HistoricalBacklog {
    private final Map<Integer, Integer> backlog;

    public static HistoricalBacklog emptyBacklog() {
        return new HistoricalBacklog(emptyMap());
    }

    public Integer get(ZonedDateTime date) {
        return backlog.get(DateUtils.minutesFromWeekStart(date));
    }

    public Integer getOr(ZonedDateTime date, Integer other) {
        return backlog.getOrDefault(DateUtils.minutesFromWeekStart(date), other);
    }
}
