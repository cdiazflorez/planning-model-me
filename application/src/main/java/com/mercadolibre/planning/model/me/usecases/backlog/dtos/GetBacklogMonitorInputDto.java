package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

import java.time.Instant;

@Value
public class GetBacklogMonitorInputDto {
    private Instant requestDate;
    private String warehouseId;
    private Workflow workflow;
    private Instant dateFrom;
    private Instant dateTo;
    private Long callerId;
}
