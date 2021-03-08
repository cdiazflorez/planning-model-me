package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.Paging;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import lombok.Value;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class UnitReportAggregationResponse {
    private Paging paging;
    private List<UnitAggregationResponse> aggregations;

    public List<Backlog> mapToBacklog(String aggregationName, ZoneId timezone) {
        return this.aggregations.stream()
                .filter(aggregationResponse ->
                        aggregationResponse.getName().equalsIgnoreCase(aggregationName))
                .map(UnitAggregationResponse::getTotals)
                .flatMap(Collection::stream)
                .map(total -> total.toBacklog(timezone))
                .collect(Collectors.toList());
    }
}
