package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

import java.util.List;

@Value
public class StaffingResponse {

    private List<Aggregation> aggregations;
}
