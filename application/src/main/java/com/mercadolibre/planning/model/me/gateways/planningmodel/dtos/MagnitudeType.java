package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MagnitudeType {
    ORDER_UNITS(null),
    PRODUCTIVITY("Productividad efectiva"),
    HEADCOUNT("Headcount"),
    THROUGHPUT("Throughput"),
    MAX_CAPACITY("Throughput"),
    BACKLOG(null),
    ORDER_DISTRIBUTION(null),
    REMAINING_PROCESSING(null),
    BACKLOG_UPPER_LIMIT(null),
    BACKLOG_LOWER_LIMIT(null);

    private String title;

    public static MagnitudeType from(final String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }
}
