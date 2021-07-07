package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

import java.util.List;

@Value
public class PlannedHeadcount {
    private List<PlannedHeadcountByHour> headcountByHours;
}
