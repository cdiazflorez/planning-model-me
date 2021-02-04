package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DeviationInput;

import lombok.AllArgsConstructor;

import javax.inject.Named;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

@Named
@AllArgsConstructor
public class SaveDeviation implements UseCase<DeviationInput, DeviationResponse> {

    private final PlanningModelGateway planningModelGateway;

    @Override
    public DeviationResponse execute(DeviationInput input) {

        try {
            return planningModelGateway
                    .saveDeviation(input).toBuilder()
                    .message("Forecast deviation saved")
                    .build();
        } catch (Exception e) {
            return new DeviationResponse(SC_INTERNAL_SERVER_ERROR,
                    "Error persisting forecast deviation");
        }
    }
}
