package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchUnitFilterRequestStringValueTest {

    @Test
    public void testFilterStringValue() {

        // GIVEN
        final SearchUnitFilterRequestStringValue filterValue =
                SearchUnitFilterRequestStringValue.from("CARRIER_ID");

        // WHEN
        final String value = filterValue.toJson();

        // THEN
        assertEquals("carrier_id", value);
    }


    @Test
    public void testFilterValueFromString() {

        // WHEN
        final SearchUnitFilterRequestStringValue filterValue =
                SearchUnitFilterRequestStringValue.from("CARRIER_ID");

        // THEN
        assertEquals(SearchUnitFilterRequestStringValue.CARRIER_ID, filterValue);
    }
}
