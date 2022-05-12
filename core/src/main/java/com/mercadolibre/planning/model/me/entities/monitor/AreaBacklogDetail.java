package com.mercadolibre.planning.model.me.entities.monitor;

import java.util.List;
import lombok.Value;

/** The backlog at some area. */
@Value
public class AreaBacklogDetail {
    String id;

    UnitMeasure value;

    List<SubAreaBacklogDetail> subareas;

    @Value
    public static class SubAreaBacklogDetail {
      String id;

      UnitMeasure value;
    }
}
