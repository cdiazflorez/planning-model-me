package com.mercadolibre.planning.model.me.gateways.planningmodel;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import java.util.List;

public interface DeviationGateway {

    List<Deviation> getActiveDeviations(Workflow workflow, String warehouseId, ZonedDateTime date);

}
