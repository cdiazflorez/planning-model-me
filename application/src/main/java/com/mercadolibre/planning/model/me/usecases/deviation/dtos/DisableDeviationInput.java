package com.mercadolibre.planning.model.me.usecases.deviation.dtos;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DisableDeviationInput {

  String warehouseId;

  Workflow workflow;

  DeviationType type;

  List<ShipmentType> affectedShipmentTypes;
}
