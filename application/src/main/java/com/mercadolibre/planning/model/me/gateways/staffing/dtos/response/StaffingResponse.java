package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import java.util.List;
import lombok.Value;

@Value
public class StaffingResponse {

    private List<StaffingWorkflowResponse> workflows;

}
