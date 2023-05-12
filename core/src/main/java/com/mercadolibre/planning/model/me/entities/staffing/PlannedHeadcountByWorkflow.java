package com.mercadolibre.planning.model.me.entities.staffing;

import java.util.List;
import lombok.Value;

@Value
public class PlannedHeadcountByWorkflow {
    private String workflow;
    private Integer totalWorkers;
    private List<PlannedHeadcountByProcess> processes;
}
