package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.util.List;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class SaveDeviation {
  private final DeviationGateway deviationGateway;


  public void execute(final List<SaveDeviationInput> deviations) {
    deviationGateway.save(deviations);
  }
}
