package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Forecast {
    private List<Metadata> metadata;
    private List<ProcessingDistribution> processingDistributions;
    private List<HeadcountDistribution> headcountDistributions;
    private List<PolyvalentProductivity> polyvalentProductivities;
    private List<HeadcountProductivity> headcountProductivities;
    private List<PlanningDistribution> planningDistributions;
    private List<BacklogLimit> backlogLimits;
    private long userID;
}
