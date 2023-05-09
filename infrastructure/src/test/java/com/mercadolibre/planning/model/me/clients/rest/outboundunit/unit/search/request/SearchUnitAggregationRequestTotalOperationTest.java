package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SearchUnitAggregationRequestTotalOperationTest {

    @Test
    public void testLookupValue() {

        // GIVEN
        final SearchUnitAggregationRequestTotalOperation operation =
                SearchUnitAggregationRequestTotalOperation.from("SUM");

        // WHEN
        final String value = operation.toJson();

        // THEN
        assertEquals("sum", value);
    }
}
