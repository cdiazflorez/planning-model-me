package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SearchUnitAggregationFilterRequest {

    private List<Map<String, Object>> and;
}
