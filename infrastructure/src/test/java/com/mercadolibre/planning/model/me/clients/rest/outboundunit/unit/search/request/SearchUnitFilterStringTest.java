package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.CARDINALITY;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchUnitFilterStringTest {

    @Test
    @DisplayName("toMap works")
    public void test() {
        // GIVEN
        final SearchUnitFilterRequest searchUnitFilterRequest =
                SearchUnitFilterRequest.string(CARDINALITY, "mono");

        // WHEN
        final Map<String, Object> map = searchUnitFilterRequest.toMap();

        // THEN
        final Map<String, Object> expectedMap = Collections.singletonMap("cardinality", "mono");

        assertEquals(expectedMap, map);
    }
}
