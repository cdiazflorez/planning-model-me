package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

/** The backlog at some area. */
@Value
public class AreaBacklogDetail {
    private String id;
    private UnitMeasure value;
}
