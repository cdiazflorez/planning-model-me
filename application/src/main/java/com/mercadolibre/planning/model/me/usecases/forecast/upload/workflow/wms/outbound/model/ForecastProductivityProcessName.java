package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ForecastProductivityProcessName {
    PICKING(2),
    PACKING(3),
    EXPEDITION(4);

    private final int columnIndex;

    public String getName() {
        return name().toLowerCase();
    }

    public static Stream<ForecastProductivityProcessName> stream() {
        return Stream.of(ForecastProductivityProcessName.values());
    }
}
