package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

@Value
public class WorkflowTotals {
    private Integer idle;
    private Integer workingSystemic;
    private Integer workingNonSystemic;
}
