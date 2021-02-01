package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class PlanningDistributionRequest {
    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateInFrom;
    private ZonedDateTime dateInTo;
    private ZonedDateTime dateOutFrom;
    private ZonedDateTime dateOutTo;

    public PlanningDistributionRequest(String warehouseId,
                                       Workflow workflow,
                                       ZonedDateTime dateInTo,
                                       ZonedDateTime dateOutFrom,
                                       ZonedDateTime dateOutTo) {
        this.warehouseId = warehouseId;
        this.workflow = workflow;
        this.dateInTo = dateInTo;
        this.dateOutFrom = dateOutFrom;
        this.dateOutTo = dateOutTo;
    }
}
