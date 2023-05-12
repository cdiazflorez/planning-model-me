package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchUnitAggregationFilterRequest {

    private List<Map<String, Object>> and;
}
