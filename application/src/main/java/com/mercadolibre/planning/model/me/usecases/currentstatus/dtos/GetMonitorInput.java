package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@SuperBuilder
@Getter
public class GetMonitorInput {

    private final String warehouseId;
    private final Workflow workflow;
    private final ZonedDateTime dateTo;
    private final ZonedDateTime dateFrom;

}
