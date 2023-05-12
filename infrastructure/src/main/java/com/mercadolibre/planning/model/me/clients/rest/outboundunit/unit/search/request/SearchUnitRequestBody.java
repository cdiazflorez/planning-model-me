package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
