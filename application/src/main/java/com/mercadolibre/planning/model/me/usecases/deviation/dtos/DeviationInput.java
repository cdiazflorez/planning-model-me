package com.mercadolibre.planning.model.me.usecases.deviation.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder(toBuilder = true)
public class DeviationInput {

    private Workflow workflow;
    private String warehouseId;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Double value;
    private long userId;

}
