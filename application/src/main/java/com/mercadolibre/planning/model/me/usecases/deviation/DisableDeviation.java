package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class DisableDeviation {

  private final DeviationGateway planningModelGateway;

  public void execute(DisableDeviationInput input) {
    planningModelGateway.disableDeviation(input);
  }
}
