package com.mercadolibre.planning.model.me.entities.monitor;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class ProcessBacklogDetail {
    private ZonedDateTime date;
    private UnitMeasure target;
    private UnitMeasure total;
    private List<AreaBacklogDetail> areas;
}
