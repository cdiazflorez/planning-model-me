package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UnitReportAggregationRequest {
    private final String name;
    private final ReportAggregationsKeys key;
    private final List<ReportAggregationsTotals> totals;
}
