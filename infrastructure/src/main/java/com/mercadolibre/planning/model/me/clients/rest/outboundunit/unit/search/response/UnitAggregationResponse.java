package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import java.util.List;
import lombok.Value;

@Value
public class UnitAggregationResponse {
    private String name;
    private List<AggregationTotal> totals;
}
