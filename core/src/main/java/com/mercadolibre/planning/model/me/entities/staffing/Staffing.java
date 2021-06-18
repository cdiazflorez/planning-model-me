package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Staffing {

    private Integer totalWorkers;

    private Integer plannedWorkers;

    private Integer globalNetProductivity;

    private List<StaffingWorkflow> workflows;
}
