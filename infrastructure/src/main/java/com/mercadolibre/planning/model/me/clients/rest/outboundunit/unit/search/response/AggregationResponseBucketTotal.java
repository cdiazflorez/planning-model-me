package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AggregationResponseBucketTotal {

    private String alias;
    private long result;
}

