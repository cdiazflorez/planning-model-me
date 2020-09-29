package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastHeadcountArea {
    MZ(2),
    RS(3),
    BL(4),
    RK(5),
    HV(6);

    private final int columnIndex;

    public String getName() {
        return name().toLowerCase();
    }

}
