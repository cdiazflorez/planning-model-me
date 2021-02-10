package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class SuggestedWavesRequest {
    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Integer backlog;
    private boolean applyDeviation;
}
