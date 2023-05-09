package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.GROUP_TYPE;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.WAREHOUSE_ID;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SearchUnitFilterTest {

    @Test
    public void empty() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.empty();

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestEmpty);
    }

    @Test
    public void and() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.and();

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestList);
        assertEquals(
                SearchUnitFilterRequestListValue.AND,
                ((SearchUnitFilterRequestList) result).getKey()
        );
    }

    @Test
    public void andWithElements() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.and(
                SearchUnitFilterRequest.string(GROUP_TYPE, "order"),
                SearchUnitFilterRequest.string(WAREHOUSE_ID, "BRSP01")
        );

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestList);
        assertEquals(
                SearchUnitFilterRequestListValue.AND,
                ((SearchUnitFilterRequestList) result).getKey()
        );
    }

    @Test
    public void or() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.or();

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestList);
        assertEquals(
                SearchUnitFilterRequestListValue.OR,
                ((SearchUnitFilterRequestList) result).getKey()
        );
    }

    @Test
    public void orWithElements() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.or(
                SearchUnitFilterRequest.string(GROUP_TYPE, "order"),
                SearchUnitFilterRequest.string(WAREHOUSE_ID, "BRSP01")
        );

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestList);
        assertEquals(
                SearchUnitFilterRequestListValue.OR,
                ((SearchUnitFilterRequestList) result).getKey()
        );
    }

    @Test
    @DisplayName("list - empty")
    public void joinListEmpty() {
        // WHEN
        final SearchUnitFilterRequest result =
                SearchUnitFilterRequest.join(SearchUnitFilterRequestListValue.AND, emptyList());

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestEmpty);
    }

    @Test
    @DisplayName("list - one element")
    public void joinListOneElement() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.join(
                SearchUnitFilterRequestListValue.AND,
                singletonList(SearchUnitFilterRequest.string(GROUP_TYPE, "order"))
        );

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestString);
    }

    @Test
    @DisplayName("list - two elements")
    public void joinListTwoElements() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.join(
                SearchUnitFilterRequestListValue.AND,
                asList(
                        SearchUnitFilterRequest.string(GROUP_TYPE, "order"),
                        SearchUnitFilterRequest.string(WAREHOUSE_ID, "BRSP01")
                )
        );

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestList);
        assertEquals(
                SearchUnitFilterRequestListValue.AND,
                ((SearchUnitFilterRequestList) result).getKey()
        );
    }

    @Test
    public void not() {
        // WHEN
        final SearchUnitFilterRequest result =
                SearchUnitFilterRequest.not(SearchUnitFilterRequest.string(GROUP_TYPE, "order"));

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestSingleton);
        assertEquals(
                SearchUnitFilterRequestSingletonValue.NOT,
                ((SearchUnitFilterRequestSingleton) result).getKey()
        );
    }

    @Test
    public void notIsEmpty() {
        // WHEN
        final SearchUnitFilterRequest result =
                SearchUnitFilterRequest.not(SearchUnitFilterRequest.empty());

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestEmpty);
    }

    @Test
    public void string() {
        // WHEN
        final SearchUnitFilterRequest result = SearchUnitFilterRequest.string(GROUP_TYPE, "order");

        // THEN
        assertTrue(result instanceof SearchUnitFilterRequestString);
    }
}
