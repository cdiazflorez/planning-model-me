package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnalyticsQueryEvent {

    PICKING("Picking"),
    PACKING_WALL("Packing Wall"),
    PACKING_NO_WALL("Packing");
    
    private final String relatedProcess;
    
    public String getName() {
        return name().toUpperCase();
    }

    public static AnalyticsQueryEvent fromString(final String process) {
        return valueOf(process.toUpperCase());
    }
    
}
