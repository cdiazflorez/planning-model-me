package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InboundForecast extends Forecast {
    private List<ProcessingDistribution> processingDistributions;
    private List<HeadcountProductivity> headcountProductivities;
    private List<PolyvalentProductivity> polyvalentProductivities;
    private List<BacklogLimit> backlogLimits;
}
