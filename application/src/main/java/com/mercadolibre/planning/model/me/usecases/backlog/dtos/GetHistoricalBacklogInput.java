package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class GetHistoricalBacklogInput {
    private Instant requestDate;
    private String warehouseId;
    private Workflow workflow;
    private List<ProcessName> processes;
    private Instant dateFrom;
    private Instant dateTo;
}
