package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
class SearchUnitRequestBody {

    private int limit;
    private long offset;
    private Map<String, Object> filter;
    private List<SearchUnitAggregationRequest> aggregations;
    private List<SearchUnitSorter> sorters;
}
