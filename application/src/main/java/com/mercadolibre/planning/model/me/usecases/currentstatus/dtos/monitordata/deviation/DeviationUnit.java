package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeviationUnit {
    
    private final String title;
    private final String value;
    DeviationUnitDetail detail;
}
