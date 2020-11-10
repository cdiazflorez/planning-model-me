package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.CARDINALITY;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.CARRIER_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchUnitFilterListTest {

    @Test
    @DisplayName("toMap works")
    public void test() {
        // GIVEN
        final SearchUnitFilterRequest searchUnitFilterRequestList =
                SearchUnitFilterRequest.and(
                        SearchUnitFilterRequest.string(CARDINALITY, "mono"),
                        SearchUnitFilterRequest.string(CARRIER_NAME, "correios")
                );

        // WHEN
        final Map<String, Object> map = searchUnitFilterRequestList.toMap();

        // THEN
        final Map<String, Object> expectedMap = ImmutableMap.<String, Object>builder()
                .put("and", asList(
                        singletonMap("cardinality", "mono"),
                        singletonMap("carrier_name", "correios")
                ))
                .build();

        assertEquals(expectedMap, map);
    }
}

