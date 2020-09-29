package com.mercadolibre.planning.model.me.usecases.forecast.upload;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

@Named
@AllArgsConstructor
public class CreateForecast implements UseCase<ForecastDto, ForecastResponse> {

    private final PlanningModelGateway planningModelGateway;

    @Override
    public ForecastResponse execute(final ForecastDto input) {
        return planningModelGateway.postForecast(input.getWorkflow(), input.getForecast());
    }
}
