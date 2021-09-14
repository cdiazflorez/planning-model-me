package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetHistoricalBacklogInput {
    private String warehouseId;
    private List<String> workflows;
    private List<ProcessName> processes;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
}
