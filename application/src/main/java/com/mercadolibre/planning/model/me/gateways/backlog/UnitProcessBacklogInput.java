package com.mercadolibre.planning.model.me.gateways.backlog;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

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
