package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetBacklogLimitsInput {
    String warehouseId;
    Workflow workflow;
    List<ProcessName> processes;
    Instant dateFrom;
    Instant dateTo;
}
