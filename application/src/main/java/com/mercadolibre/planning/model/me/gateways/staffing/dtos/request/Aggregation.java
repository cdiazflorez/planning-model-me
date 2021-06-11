package com.mercadolibre.planning.model.me.gateways.staffing.dtos.request;

import lombok.Value;

import java.util.List;

@Value
public class Aggregation {

    final String name;

    final List<String> keys;

    final List<Operation> operations;
}
