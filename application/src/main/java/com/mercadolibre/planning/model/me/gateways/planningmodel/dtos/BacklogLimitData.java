package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BacklogLimitData {
    private ZonedDateTime date;
    private Integer quantity;
}
