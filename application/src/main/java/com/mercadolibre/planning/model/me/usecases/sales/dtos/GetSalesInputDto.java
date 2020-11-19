package com.mercadolibre.planning.model.me.usecases.sales.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class GetSalesInputDto {
    private Workflow workflow;
    private String warehouseId;
    private ZonedDateTime dateFrom;
}
