package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SearchUnitAggregationRequestTotal {

    private SearchUnitAggregationRequestTotalOperation operation;
    private String operand;
    private String alias;
}
