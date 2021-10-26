package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

import java.util.List;

@Value
public class StaffingWorkflowResponse {
    private String name;
    private WorkflowTotals totals;
    private List<StaffingProcess> processes;
}
