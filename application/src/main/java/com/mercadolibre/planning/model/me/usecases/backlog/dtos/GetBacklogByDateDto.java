package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import lombok.Value;

@Value
public class GetBacklogByDateDto {

    private Workflow workflow;

    private String warehouseId;

    private Instant dateFrom;

    private Instant dateTo;
}
