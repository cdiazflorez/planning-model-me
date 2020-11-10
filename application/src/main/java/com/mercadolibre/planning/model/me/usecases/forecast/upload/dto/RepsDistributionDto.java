package com.mercadolibre.planning.model.me.usecases.forecast.upload.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RepsDistributionDto {
    final List<ProcessingDistribution> processingDistributions;
    final List<HeadcountProductivity> headcountProductivities;
}
