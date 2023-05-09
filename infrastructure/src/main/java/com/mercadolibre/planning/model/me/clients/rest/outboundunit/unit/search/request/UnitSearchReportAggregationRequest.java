package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UnitSearchReportAggregationRequest {
    final Map<String, String> paging;
    final Map<String, Object> filters;
    final List<UnitReportAggregationRequest> aggregations;

}
