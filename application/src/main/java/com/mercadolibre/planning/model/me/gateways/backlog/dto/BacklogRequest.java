package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@RequiredArgsConstructor
public class BacklogRequest {
    private Instant requestDate;
    private String warehouseId;
    private List<String> workflows;
    private List<String> processes;
    private List<String> groupingFields;
    private Instant dateFrom;
    private Instant dateTo;
}
