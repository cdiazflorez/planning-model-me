package com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountArea.BL;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountArea.HV;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountArea.MZ;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountArea.RK;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastHeadcountArea.RS;

@Getter
@AllArgsConstructor
public enum ForecastHeadcountProcessName {
    PICKING(List.of(MZ, RS, BL, RK, HV), 178);

    private final List<ForecastHeadcountArea> areas;
    private final int rowIndex;

    public String getName() {
        return name().toLowerCase();
    }

}
