package com.mercadolibre.planning.model.me.gateways.staffing.dtos.request;

import lombok.Value;

@Value
public class Operation {

    private String alias;

    private String operand;

    private String operator;
}
