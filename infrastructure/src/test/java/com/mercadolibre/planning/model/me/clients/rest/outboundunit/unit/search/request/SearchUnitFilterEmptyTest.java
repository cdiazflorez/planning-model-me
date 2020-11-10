package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchUnitFilterEmptyTest {

    @Test
    @DisplayName("toMap works")
    public void test() {
        // GIVEN
        final SearchUnitFilterRequest searchUnitFilter = SearchUnitFilterRequest.empty();

        // WHEN
        final Map<String, Object> map = searchUnitFilter.toMap();

        // THEN
        final Map<String, Object> expectedMap = emptyMap();

        assertEquals(expectedMap, map);
    }
}
