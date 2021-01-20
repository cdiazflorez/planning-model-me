package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeviationUnitDetail {
    
    private final Metric forecastUnits;
    private final Metric currentUnits;
}
