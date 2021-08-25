package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class BacklogRequest {
    private String warehouseId;
    private List<String> workflows;
    private List<String> processes;
    private List<String> groupingFields;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
}
