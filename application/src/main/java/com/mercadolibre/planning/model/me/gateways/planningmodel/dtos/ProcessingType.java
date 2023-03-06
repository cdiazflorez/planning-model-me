package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ProcessingType {
    ACTIVE_WORKERS,
    EFFECTIVE_WORKERS,
    ACTIVE_WORKERS_NS,
    EFFECTIVE_WORKERS_NS,
    PERFORMED_PROCESSING,
    REMAINING_PROCESSING,
    WORKERS,
    WORKERS_NS,
    THROUGHPUT,
    MAX_CAPACITY,
    PRODUCTIVITY,
    BACKLOG_LOWER_LIMIT,
    BACKLOG_UPPER_LIMIT;

    @JsonCreator
    public static ProcessingType from(final String value) {
        return Arrays.stream(values())
                .filter(v -> v.getName().equals(value))
                .findFirst()
                .orElse(null);
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }
}
