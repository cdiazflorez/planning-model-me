package com.mercadolibre.planning.model.me.usecases.forecast.upload;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelForecastGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

@Named
@AllArgsConstructor
public class CreateForecast implements UseCase<ForecastDto, ForecastResponse> {

    private final PlanningModelForecastGateway planningModelForecastGateway;

    @Override
    public ForecastResponse execute(final ForecastDto input) {
        return planningModelForecastGateway.postForecast(input.getWorkflow(), input.getForecast());
    }
}
