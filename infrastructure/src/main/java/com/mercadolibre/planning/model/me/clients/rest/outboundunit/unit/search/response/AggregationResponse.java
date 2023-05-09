package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AggregationResponse {

    private String name;
    private List<AggregationResponseBucket> buckets;
}
