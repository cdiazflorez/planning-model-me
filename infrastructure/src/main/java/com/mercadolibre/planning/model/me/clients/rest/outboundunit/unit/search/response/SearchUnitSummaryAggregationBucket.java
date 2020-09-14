package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SearchUnitSummaryAggregationBucket {

    private List<String> keys;
    private long total;
}
