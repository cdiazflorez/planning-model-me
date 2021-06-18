package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessName {
    PICKING(1),
    PACKING(2),
    PACKING_WALL(3),
    WAVING(null),
    BATCH_SORTER(null),
    WALL_IN(null);

    private final Integer index;

    @JsonCreator
    public static ProcessName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }

}
