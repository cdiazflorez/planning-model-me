package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SearchUnitRequest {

    private int limit;
    private long offset;
    private SearchUnitAggregationFilterRequest filter;
    private List<SearchUnitAggregationRequest> aggregations;
    private List<SearchUnitSorter> sorters;
}
