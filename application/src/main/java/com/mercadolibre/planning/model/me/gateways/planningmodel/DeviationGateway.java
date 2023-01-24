package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import java.util.List;
import java.util.Set;

public interface DeviationGateway {

  List<Deviation> getActiveDeviations(Set<Workflow> workflows, String warehouseId, Instant date);
  DeviationResponse disableDeviation(DisableDeviationInput saveDeviationInput);

}
