package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OutboundForecast extends Forecast {
    private Set<SheetParser> sheets;
    private List<ProcessingDistribution> processingDistributions;
    private List<HeadcountDistribution> headcountDistributions;
    private List<PolyvalentProductivity> polyvalentProductivities;
    private List<HeadcountProductivity> headcountProductivities;
    private List<PlanningDistribution> planningDistributions;
    private List<BacklogLimit> backlogLimits;
}
