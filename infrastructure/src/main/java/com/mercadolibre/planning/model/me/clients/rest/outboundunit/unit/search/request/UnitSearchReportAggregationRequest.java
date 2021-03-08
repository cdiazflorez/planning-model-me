package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
public class UnitSearchReportAggregationRequest {
    final Map<String, String> paging;
    final Map<String, Object> filters;
    final List<UnitReportAggregationRequest> aggregations;

}
