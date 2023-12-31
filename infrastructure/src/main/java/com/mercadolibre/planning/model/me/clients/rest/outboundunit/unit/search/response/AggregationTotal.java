package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response;

import static java.time.ZoneOffset.UTC;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import java.time.Instant;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class AggregationTotal {
    private Long fieldValue;
    private AggregationTotalDetail totals;

    public Backlog toBacklog() {
        return new Backlog(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(fieldValue), UTC),
                totals.getCountResultsByKey());
    }
}
