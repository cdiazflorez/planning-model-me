package com.mercadolibre.planning.model.me.gateways.backlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@AllArgsConstructor
@Value
public class UnitProcessBacklogInput {

    String statuses;
    String warehouseId;
    ZonedDateTime dateFrom;
    ZonedDateTime dateTo;
    String area;
    String groupType;
}
