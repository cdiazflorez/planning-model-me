package com.mercadolibre.planning.model.me.usecases.forecast.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RepsDistributionDto {
    final List<ProcessingDistribution> processingDistributions;
    final List<HeadcountProductivity> headcountProductivities;
}
