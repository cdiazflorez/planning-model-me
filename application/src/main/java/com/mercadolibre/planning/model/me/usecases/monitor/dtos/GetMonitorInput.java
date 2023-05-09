package com.mercadolibre.planning.model.me.usecases.monitor.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GetMonitorInput {

    private final String warehouseId;
    private final Workflow workflow;
    private final ZonedDateTime dateTo;
    private final ZonedDateTime dateFrom;
}
