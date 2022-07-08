package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ForecastProductivityProcessName {
    PICKING(2),
    BATCH_SORTER(3),
    WALL_IN(4),
    PACKING(5),
    PACKING_WALL(6);

    private final int columnIndex;

    public String getName() {
        return name().toLowerCase();
    }

    public static Stream<ForecastProductivityProcessName> stream() {
        return Stream.of(ForecastProductivityProcessName.values());
    }
}
