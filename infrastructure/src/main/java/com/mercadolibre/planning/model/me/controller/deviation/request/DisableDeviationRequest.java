package com.mercadolibre.planning.model.me.controller.deviation.request;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class DisableDeviationRequest {

  @NotNull
  Workflow workflow;

  List<ShipmentType> affectedShipmentTypes;

  public DisableDeviationInput toDeviationInput(final DeviationType type) {

    return DisableDeviationInput.builder()
        .workflow(workflow)
        .type(type)
        .affectedShipmentTypes(affectedShipmentTypes)
        .build();
  }
}
