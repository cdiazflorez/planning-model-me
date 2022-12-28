package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class SaveOutboundDeviation implements UseCase<SaveDeviationInput, DeviationResponse> {

  private final PlanningModelGateway planningModelGateway;

  @Override
  public DeviationResponse execute(SaveDeviationInput input) {
    return planningModelGateway
        .saveDeviation(input).toBuilder()
        .message("Forecast deviation saved")
        .build();
  }
}
