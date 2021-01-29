package com.mercadolibre.planning.model.me.controller.deviation.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DeviationInput;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
@Builder(toBuilder = true)
public class DeviationRequest {

    @NotEmpty
    private String warehouseId;
    @NotNull
    private ZonedDateTime dateFrom;
    @NotNull
    private ZonedDateTime dateTo;
    @NotNull
    private Double value;
    @NotNull
    private long userId;

    public DeviationInput toDeviationInput(final Workflow workflow) {
        return DeviationInput.builder()
                .workflow(workflow)
                .warehouseId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .value(value)
                .userId(userId)
                .build();
    }
}
