package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

@Value
public class Totals {
    private Integer idle;
    private Integer workingSystemic;
    private Double productivity;
    private Double throughput;
}
