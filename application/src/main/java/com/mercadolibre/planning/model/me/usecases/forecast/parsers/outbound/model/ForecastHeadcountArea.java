package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastHeadcountArea {
    MZ(2),
    ESTANDAR(2),
    RS(3),
    PTW(3),
    BL(4),
    VOLUMINOSO(4),
    RK_H(5),
    CPG(5),
    RK_L(6),
    HV(7);

    private final int columnIndex;

    public String getName() {
        return name().toLowerCase();
    }

}
