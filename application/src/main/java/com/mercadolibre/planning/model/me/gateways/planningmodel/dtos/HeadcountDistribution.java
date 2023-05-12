package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HeadcountDistribution {
    private String processName;
    private String quantityMetricUnit;
    private List<AreaDistribution> areas;
}
