package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class GetBacklogMonitorInputDto {
    private String warehouseId;
    private String workflow;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Long callerId;
}
