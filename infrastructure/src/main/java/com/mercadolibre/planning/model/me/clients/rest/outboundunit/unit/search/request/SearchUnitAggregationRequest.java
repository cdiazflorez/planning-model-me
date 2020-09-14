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
public class SearchUnitAggregationRequest {

    private String name;
    private List<SearchUnitAggregationRequestTotal> totals;
}
