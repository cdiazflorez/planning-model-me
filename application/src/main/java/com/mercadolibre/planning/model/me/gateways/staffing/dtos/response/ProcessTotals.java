package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

@Value
public class ProcessTotals {
    private Integer idle;
    private Integer workingSystemic;
    private Double netProductivity;
    private Double effProductivity;
    private Double throughput;
}
