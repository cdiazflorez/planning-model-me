package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

@AllArgsConstructor
@Data
class SearchUnitFilterRequestList implements SearchUnitFilterRequest {

    private SearchUnitFilterRequestListValue key;
    private List<SearchUnitFilterRequest> values;

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        return singletonMap(
                key.name().toLowerCase(),
                values.stream().map(SearchUnitFilterRequest::toMap).collect(Collectors.toList())
        );
    }
}
