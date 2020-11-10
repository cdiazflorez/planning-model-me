package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class PlanningDistributionRequest {
    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateInTo;
    private ZonedDateTime dateOutFrom;
    private ZonedDateTime dateOutTo;
}
