package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

import static java.util.Collections.emptyMap;

@AllArgsConstructor
@Data
class SearchUnitFilterRequestEmpty implements SearchUnitFilterRequest {

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Map<String, Object> toMap() {
        return emptyMap();
    }
}
