package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class Metric {
    private final String type;
    private final String title;
    private final String subtitle;
    private final String value;
    private final String status;
    private final String icon;    
}
