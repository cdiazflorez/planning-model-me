package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.Paging;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class UnitReportAggregationResponse {
    private Paging paging;
    private List<UnitAggregationResponse> aggregations;

    public List<Backlog> mapToBacklog(String aggregationName) {
        return this.aggregations.stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(aggregationName))
                .map(UnitAggregationResponse::getTotals)
                .flatMap(Collection::stream)
                .map(AggregationTotal::toBacklog)
                .collect(Collectors.toList());
    }
}
