package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AggregationResponseBucket {

    private List<String> keys;
    private List<AggregationResponseBucketTotal> totals;

    public SearchUnitSummaryAggregationBucket toSummaryBucket(final String aliasName) {
        final Optional<Long> optionalResult = totals.stream()
                .filter(total -> total.getAlias().equals(aliasName))
                .map(AggregationResponseBucketTotal::getResult)
                .findFirst();

        return SearchUnitSummaryAggregationBucket.builder()
                .keys(keys)
                .total(optionalResult.orElseGet(() -> 0L))
                .build();
    }
}
