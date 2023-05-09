package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import java.util.List;
import lombok.Value;

@Value
public class StaffingWorkflowResponse {
    private String name;
    private WorkflowTotals totals;
    private List<StaffingProcess> processes;
}
