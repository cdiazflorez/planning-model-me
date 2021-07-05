package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

import java.util.List;

@Value
public class PlannedHeadcountByWorkflow {
    private String workflow;
    private Integer totalWorkers;
    private List<PlannedHeadcountByProcess> processes;
}
