package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class PlanningDistribution {
    private ZonedDateTime dateIn;
    private ZonedDateTime dateOut;
    private long quantity;
    private String quantityMetricUnit;
    private List<Metadata> metadata;
}
