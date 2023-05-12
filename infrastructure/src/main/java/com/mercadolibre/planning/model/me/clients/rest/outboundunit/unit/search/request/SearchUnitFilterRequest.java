package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface SearchUnitFilterRequest {
    Map<String, Object> toMap();

    default boolean isEmpty() {
        return false;
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    static SearchUnitFilterRequest empty() {
        return new SearchUnitFilterRequestEmpty();
    }

    static SearchUnitFilterRequest and(final SearchUnitFilterRequest... filters) {
        return new SearchUnitFilterRequestList(
                SearchUnitFilterRequestListValue.AND,
                Arrays.asList(filters)
        );
    }

    static SearchUnitFilterRequest or(final SearchUnitFilterRequest... filters) {
        return new SearchUnitFilterRequestList(
                SearchUnitFilterRequestListValue.OR,
                Arrays.asList(filters)
        );
    }

    static SearchUnitFilterRequest not(final SearchUnitFilterRequest filter) {
        if (filter.isEmpty()) {
            return empty();
        }

        return new SearchUnitFilterRequestSingleton(
                SearchUnitFilterRequestSingletonValue.NOT,
                filter
        );
    }

    static SearchUnitFilterRequest all(final SearchUnitFilterRequest filter) {
        return new SearchUnitFilterRequestSingleton(
                SearchUnitFilterRequestSingletonValue.ALL,
                filter
        );
    }

    static SearchUnitFilterRequest string(final SearchUnitFilterRequestStringValue name,
                                          final String value) {
        return new SearchUnitFilterRequestString(name, value);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    static SearchUnitFilterRequest join(
            final SearchUnitFilterRequestListValue key,
            final List<SearchUnitFilterRequest> filters) {

        final List<SearchUnitFilterRequest> searchFilters = filters.stream()
                .filter(SearchUnitFilterRequest::isNotEmpty)
                .collect(toList());

        if (searchFilters.isEmpty()) {
            return empty();
        }

        if (searchFilters.size() == 1) {
            return searchFilters.get(0);
        }

        return new SearchUnitFilterRequestList(key, searchFilters);
    }
}

