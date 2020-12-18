package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnalyticsQueryEvent {

    PICKUP_FINISH("Picking"),
    PACKING_FINISH("Packing");
    
    private String relatedProcess;
    
    public String getName() {
        return name().toLowerCase();
    }

    public static AnalyticsQueryEvent fromString(String process) {
        return valueOf(process.toUpperCase());
    }
}
