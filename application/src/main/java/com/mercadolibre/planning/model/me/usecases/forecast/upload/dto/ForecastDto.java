package com.mercadolibre.planning.model.me.usecases.forecast.upload.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForecastDto {
    private Workflow workflow;
    private Forecast forecast;
}
