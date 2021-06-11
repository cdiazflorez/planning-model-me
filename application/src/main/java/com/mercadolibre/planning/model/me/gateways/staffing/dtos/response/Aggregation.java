package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

import java.util.List;

@Value
public class Aggregation {

    final String name;

    final List<Result> results;
}
