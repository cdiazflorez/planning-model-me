package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StaffingWorkflow {

    private String workflow;

    private Integer globalNetProductivity;

    private Integer totalWorkers;

    private NonSystemicWorkers totalNonSystemicWorkers;

    private List<Process> processes;

}
