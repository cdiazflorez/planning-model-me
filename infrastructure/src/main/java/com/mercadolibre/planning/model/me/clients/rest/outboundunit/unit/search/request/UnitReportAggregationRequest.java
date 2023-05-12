package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnitReportAggregationRequest {
    private final String name;
    private final ReportAggregationsKeys key;
    private final List<ReportAggregationsTotals> totals;
}
