package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RowName {
    PICKING(1, "Picking"),
    PACKING(2, "Packing"),
    CHECK_IN(1, "Check in"),
    PUT_AWAY(2, "Put Away"),
    PACKING_WALL(3, "Wall"),
    DEVIATION(4, "Desviación"),
    GLOBAL(5, "Capacidad Máxima");

    private final Integer index;
    private final String title;

    public static RowName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }

}
