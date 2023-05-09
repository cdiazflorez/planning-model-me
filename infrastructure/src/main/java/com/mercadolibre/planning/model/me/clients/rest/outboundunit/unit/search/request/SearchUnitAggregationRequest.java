package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SearchUnitAggregationRequest {

    private String name;
    private List<String> keys;
    private List<SearchUnitAggregationRequestTotal> totals;
}
