package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation;

import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.Metric;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class DeviationMetric {
    
    private final Metric deviationPercentage;
    
    private final DeviationUnit deviationUnits;
    
    
}
