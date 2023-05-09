package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SearchUnitSummaryAggregationBucket {

    private List<String> keys;
    private long total;
}
