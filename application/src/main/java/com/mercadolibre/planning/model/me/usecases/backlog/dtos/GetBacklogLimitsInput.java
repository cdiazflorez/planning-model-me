package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class GetBacklogLimitsInput {
    String warehouseId;
    Workflow workflow;
    List<ProcessName> processes;
    Instant dateFrom;
    Instant dateTo;
}
