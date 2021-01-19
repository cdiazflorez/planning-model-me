package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnalyticsQueryEvent {

    PICKING("Picking"),
    PACKING_WALL("Packing de wall"),
    PACKING_NO_WALL("Packing normal");

    private final String relatedProcess;

    public String getName() {
        return name().toUpperCase();
    }

    public static AnalyticsQueryEvent fromString(final String process) {
        return valueOf(process.toUpperCase());
    }

}
