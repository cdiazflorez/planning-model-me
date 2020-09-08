package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

public enum  Workflow {
    FBM_WMS_OUTBOUND;

    public static Workflow from(final String value) {
        return valueOf(value.toUpperCase().replace("-", "_"));
    }

    public String getName() {
        return name().replace("_", "-").toLowerCase();
    }
}
