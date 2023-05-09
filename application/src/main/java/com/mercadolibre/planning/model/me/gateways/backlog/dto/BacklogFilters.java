package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BacklogFilters {
    final Object statuses;
    final String warehouseId;
    final ZonedDateTime cptFrom;
    final ZonedDateTime cptTo;
    final ZonedDateTime dateCreatedFrom;
    final ZonedDateTime dateCreatedTo;
    final String groupType;
    final ZoneId timeZone;
}
