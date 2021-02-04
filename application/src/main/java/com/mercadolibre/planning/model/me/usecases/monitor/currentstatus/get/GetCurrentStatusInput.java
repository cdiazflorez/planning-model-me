package com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Builder
@Getter
public class GetCurrentStatusInput {

    private final String warehouseId;
    private final Workflow workflow;
    private final ZonedDateTime dateTo;
    private final ZonedDateTime dateFrom;
    private final ZonedDateTime currentTime;
}
