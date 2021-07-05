package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

@Value
public class PlannedHeadcountByProcess {
    private String process;
    private Integer totalWorkers;
    private Integer throughput;
}
