package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.WORKERS;
import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum ForecastProcessName {
    WAVING(List.of(PERFORMED_PROCESSING), 2),
    PICKING(List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS), 3),
    PACKING(List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS), 7),
    PACKING_WALL(List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS), 11),
    EXPEDITION(List.of(REMAINING_PROCESSING, WORKERS, ACTIVE_WORKERS), 15),
    GLOBAL(List.of(MAX_CAPACITY), 19);

    private final List<ForecastProcessType> processTypes;
    private final int startingColumn;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static Stream<ForecastProcessName> stream() {
        return Stream.of(ForecastProcessName.values());
    }

    private static final Map<String, ForecastProcessName> LOOKUP = Arrays.stream(values()).collect(
            toMap(ForecastProcessName::toString, Function.identity())
    );

    @JsonCreator
    public static ForecastProcessName from(final String status) {
        return LOOKUP.get(status.toLowerCase());
    }

}