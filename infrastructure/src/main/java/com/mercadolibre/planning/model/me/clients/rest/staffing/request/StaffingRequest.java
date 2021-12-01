package com.mercadolibre.planning.model.me.clients.rest.staffing.request;

import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.Aggregation;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class StaffingRequest {

    final Map<String, String> filters;

    final List<Aggregation> aggregations;
}
