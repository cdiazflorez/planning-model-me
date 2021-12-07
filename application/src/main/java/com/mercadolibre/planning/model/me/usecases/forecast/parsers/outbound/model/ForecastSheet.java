package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastSheet {
    WORKERS("Reps"),
    ORDER_DISTRIBUTION("Distribucion ventas");

    private final String name;
}
