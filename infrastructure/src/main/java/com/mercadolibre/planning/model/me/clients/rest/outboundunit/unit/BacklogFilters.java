package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class BacklogFilters {
    final Object statuses;
    final String warehouseId;
    final ZonedDateTime dateFrom;
    final ZonedDateTime dateTo;
    final String groupType;
}
