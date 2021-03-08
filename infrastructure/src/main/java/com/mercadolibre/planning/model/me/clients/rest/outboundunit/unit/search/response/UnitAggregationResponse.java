package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import lombok.Value;

import java.util.List;

@Value
public class UnitAggregationResponse {
    private String name;
    private List<AggregationTotal> totals;
}
