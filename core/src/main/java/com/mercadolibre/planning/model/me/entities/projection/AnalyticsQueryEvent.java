package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnalyticsQueryEvent {

    PICKING("Picking","picking"),
    PACKING_WALL("Packing de wall","packing_wall"),
    PACKING_NO_WALL("Packing normal","packing");

    private final String relatedProcess;
    private final String relatedProcessName;

    public String getName() {
        return name().toUpperCase();
    }

    public static AnalyticsQueryEvent fromString(final String process) {
        return valueOf(process.toUpperCase());
    }

}
