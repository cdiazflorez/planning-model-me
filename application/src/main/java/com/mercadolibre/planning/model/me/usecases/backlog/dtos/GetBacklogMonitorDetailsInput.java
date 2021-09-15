package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetBacklogMonitorDetailsInput {
    private String warehouseId;
    private String workflow;
    private ProcessName process;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Long callerId;
}
