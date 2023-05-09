package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static java.util.Collections.singletonMap;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
class SearchUnitFilterRequestString implements SearchUnitFilterRequest {

    private SearchUnitFilterRequestStringValue key;
    private String value;

    @Override
    public Map<String, Object> toMap() {
        return singletonMap(key.name().toLowerCase(), value);
    }
}
