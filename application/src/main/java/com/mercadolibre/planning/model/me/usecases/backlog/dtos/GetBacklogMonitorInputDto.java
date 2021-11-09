package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import lombok.Value;

import java.time.Instant;

@Value
public class GetBacklogMonitorInputDto {
    private Instant requestInstant;
    private String warehouseId;
    private String workflow;
    private Instant dateFrom;
    private Instant dateTo;
    private Long callerId;
}
