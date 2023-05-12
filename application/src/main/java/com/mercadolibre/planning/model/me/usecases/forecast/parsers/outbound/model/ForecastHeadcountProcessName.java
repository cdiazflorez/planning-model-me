package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.BL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.CPG;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.ESTANDAR;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.HV;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.MZ;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.PTW;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.RK_H;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.RK_L;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.RS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountArea.VOLUMINOSO;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ForecastHeadcountProcessName {
    PICKING(List.of(MZ, RS, BL, RK_H, RK_L, HV), 178),
    PACKING(List.of(ESTANDAR, PTW, VOLUMINOSO, CPG), 183);

    private final List<ForecastHeadcountArea> areas;
    private final int rowIndex;

    public String getName() {
        return name().toLowerCase();
    }

}
