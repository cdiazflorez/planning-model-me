package com.mercadolibre.planning.model.me.entities.projection;

public enum AnalyticsQueryEvent {

    PICKUP_FINISH,
    PACKING_FINISH;
    
    public String getName() {
        return name().toLowerCase();
    }

    public static AnalyticsQueryEvent fromString(String process) {
        return valueOf(process.toUpperCase());
    }
}
