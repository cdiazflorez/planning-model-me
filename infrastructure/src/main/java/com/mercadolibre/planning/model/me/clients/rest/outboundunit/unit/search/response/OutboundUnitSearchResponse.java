package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.Paging;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutboundUnitSearchResponse<T> {

    private Paging paging;
    private List<T> results;
    private List<AggregationResponse> aggregations;
}
