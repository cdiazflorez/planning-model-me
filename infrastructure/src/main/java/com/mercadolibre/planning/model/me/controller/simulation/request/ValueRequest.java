package com.mercadolibre.planning.model.me.controller.simulation.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class ValueRequest {

    @NotNull
    private ZonedDateTime date;

    private int quantity;

    public static QuantityByDate toQuantityByDate(final ValueRequest value) {
        return new QuantityByDate(value.getDate(), value.getQuantity());
    }
}
