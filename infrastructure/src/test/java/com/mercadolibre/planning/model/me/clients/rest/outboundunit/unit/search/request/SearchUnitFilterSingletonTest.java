package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.CARDINALITY;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.WAREHOUSE_ID;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SearchUnitFilterSingletonTest {

    @Test
    @DisplayName("toMap works")
    public void test() {
        // GIVEN
        final SearchUnitFilterRequest searchUnitFilter =
                SearchUnitFilterRequest.not(SearchUnitFilterRequest.and(
                        SearchUnitFilterRequest.string(CARDINALITY, "mono"),
                        SearchUnitFilterRequest.string(WAREHOUSE_ID, "BRSP01")
                ));

        // WHEN
        final Map<String, Object> map = searchUnitFilter.toMap();

        // THEN
        final List<Map<String, Object>> andList = asList(
                singletonMap("cardinality", "mono"),
                singletonMap("warehouse_id", "BRSP01")
        );

        final Map<String, Object> expectedMap = ImmutableMap.<String, Object>builder()
                .put("not", singletonMap("and", andList))
                .build();

        assertEquals(expectedMap, map);
    }
}
