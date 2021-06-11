package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

import java.util.List;

@Value
public class Result {

    final List<String> keys;

    final List<Operation> operations;
}
