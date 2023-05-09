package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import static java.util.Collections.emptyMap;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

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
