package com.mercadolibre.planning.model.me.controller.deviation.request;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class SaveDeviationRequest {

  @NotNull
  Workflow workflow;

  List<ShipmentType> affectedShipmentTypes;

  @NotNull
  ZonedDateTime dateFrom;

  @NotNull
  ZonedDateTime dateTo;

  @NotNull
  Double value;

  public SaveDeviationInput toDeviationInput(final String logisticCenterId,
                                             final Long callerId,
                                             final DeviationType type) {
    return new SaveDeviationInput(
        logisticCenterId,
        workflow,
        affectedShipmentTypes,
        dateFrom,
        dateTo,
        type,
        value,
        callerId
    );
  }
}
