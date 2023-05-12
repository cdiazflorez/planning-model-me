package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import java.util.List;
import lombok.Value;

@Value
public class StaffingProcess {
    private String name;
    private ProcessTotals totals;
    private List<Area> areas;
}
