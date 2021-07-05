package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

import java.util.List;

@Value
public class PlannedHeadcountByHour {
    private String hour;
    private List<PlannedHeadcountByWorkflow> workflows;
}
