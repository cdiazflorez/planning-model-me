package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import lombok.AllArgsConstructor;

import javax.inject.Named;

@Named
@AllArgsConstructor
public class DisableDeviation implements UseCase<DisableDeviationInput, DeviationResponse> {

    private final PlanningModelGateway planningModelGateway;

    @Override
    public DeviationResponse execute(DisableDeviationInput input) {
        return planningModelGateway
                .disableDeviation(input).toBuilder()
                .message("Forecast deviation disabled")
                .build();
    }
}
