package com.mercadolibre.planning.model.me.controller.simulation.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
public class ValueRequest {

    @NotNull
    private ZonedDateTime date;

    private int quantity;

    public static QuantityByDate toQuantityByDate(final ValueRequest value) {
        return new QuantityByDate(value.getDate(), value.getQuantity());
    }
}
