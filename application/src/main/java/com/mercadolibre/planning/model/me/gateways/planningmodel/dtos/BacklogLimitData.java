package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class BacklogLimitData {
    private ZonedDateTime date;
    private Integer quantity;
}
