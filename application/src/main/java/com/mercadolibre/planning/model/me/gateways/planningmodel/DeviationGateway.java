package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface DeviationGateway {

  List<Deviation> getActiveDeviations(Set<Workflow> workflows, String warehouseId, Instant date);

  void save(List<SaveDeviationInput> deviations);

  DeviationResponse disableDeviation(DisableDeviationInput saveDeviationInput);

  DeviationResponse disableDeviationAll(String logisticCenterId, List<DisableDeviationInput> disableDeviationInput);
}
