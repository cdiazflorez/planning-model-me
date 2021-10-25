package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetBacklogLimitsInput {
    String warehouseId;
    Workflow workflow;
    List<ProcessName> processes;
    ZonedDateTime dateFrom;
    ZonedDateTime dateTo;
}
