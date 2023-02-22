package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import java.util.List;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class DisableDeviation {

  private final DeviationGateway planningModelGateway;

  public void execute(final DisableDeviationInput input) {
    planningModelGateway.disableDeviation(input);
  }

  /*
   * There are two execute method, but it is temporal because in the future only we will use "executeAll" as "execute"
   */
  public void executeAll(final String logisticCenterId, final List<DisableDeviationInput> input) {
    planningModelGateway.disableDeviationAll(logisticCenterId, input);
  }
}
