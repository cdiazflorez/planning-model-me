package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HeadcountDistribution {
    private String processName;
    private String quantityMetricUnit;
    private List<AreaDistribution> areas;
}
