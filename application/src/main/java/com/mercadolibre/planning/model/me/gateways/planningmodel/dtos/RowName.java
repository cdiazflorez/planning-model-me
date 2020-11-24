package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RowName {
    PICKING(1, "Picking"),
    PACKING(2, "Packing"),
    DEVIATION(3, "Desviación");

    private final Integer index;
    private final String title;

    public static RowName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    public String getName() {
        return name().toLowerCase();
    }

}