package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import lombok.Value;

import java.util.List;

@Value
public class StaffingProcess {
    private String name;
    private ProcessTotals totals;
    private List<Area> areas;
}
