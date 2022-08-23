package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RowName {
    PICKING(1, "Picking"),
    PACKING(2, "Packing"),
    BATCH_SORTER(3, "Batch sorter"),
    WALL_IN(4, "Wall in"),
    PACKING_WALL(5, "Packing wall"),
    CHECK_IN(1, "Check in"),
    PUT_AWAY(2, "Put Away"),
    DEVIATION(6, "Desviación"),
    GLOBAL(7, "Capacidad Máxima");

    private final Integer index;
    private final String title;

    public static RowName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }

}
