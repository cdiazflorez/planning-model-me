package com.mercadolibre.planning.model.me.entities.staffing;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Staffing {

    private Integer totalWorkers;

    private Integer plannedWorkers;

    private Integer globalNetProductivity;

    private List<StaffingWorkflow> workflows;
}
