package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public enum SearchUnitAggregationRequestTotalOperation {
    SUM;

    private static final Map<String, SearchUnitAggregationRequestTotalOperation> LOOKUP =
            Arrays.stream(values())
                    .collect(
                            toMap(SearchUnitAggregationRequestTotalOperation::toString,
                                    Function.identity()
                            )
                    );

    @JsonCreator
    public static SearchUnitAggregationRequestTotalOperation from(final String value) {
        return LOOKUP.get(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.toString().toLowerCase();
    }
}
