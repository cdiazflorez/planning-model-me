package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
public class GetHistoricalBacklogInput {
    private Instant requestDate;
    private String warehouseId;
    private List<String> workflows;
    private List<ProcessName> processes;
    private Instant dateFrom;
    private Instant dateTo;
}
