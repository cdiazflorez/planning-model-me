package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Cardinality {
    MONO_ORDER_DISTRIBUTION("mono"),
    MULTI_BATCH_DISTRIBUTION("multi batch"),
    MULTI_ORDER_DISTRIBUTION("multi order")
    ;

    private final String title;

    @JsonCreator
    public static Cardinality from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }
}
