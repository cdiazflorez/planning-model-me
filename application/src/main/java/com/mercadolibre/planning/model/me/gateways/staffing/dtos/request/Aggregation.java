package com.mercadolibre.planning.model.me.gateways.staffing.dtos.request;

import java.util.List;
import lombok.Value;

@Value
public class Aggregation {

    final String name;

    final List<String> keys;

    final List<Operation> operations;
}
