package com.mercadolibre.planning.model.me.entities.staffing;

import java.util.List;
import lombok.Value;

@Value
public class PlannedHeadcount {
    private List<PlannedHeadcountByHour> headcountByHours;
}
