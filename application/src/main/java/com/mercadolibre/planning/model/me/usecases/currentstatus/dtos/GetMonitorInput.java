package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@SuperBuilder
@Data
public class GetMonitorInput {

    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateTo;
    private ZonedDateTime dateFrom;

}
