package com.mercadolibre.planning.model.me.controller.deviation.request;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class SaveDeviationRequest {

  @NotEmpty
  String warehouseId;

  @NotNull
  Workflow workflow;

  List<ShipmentType> shipmentTypes;

  @NotNull
  ZonedDateTime dateFrom;

  @NotNull
  ZonedDateTime dateTo;

  @NotNull
  Double value;

  @NotNull
  long userId;

  public SaveDeviationInput toDeviationInput(final DeviationType type) {
    return new SaveDeviationInput(
        warehouseId,
        workflow,
        shipmentTypes,
        dateFrom,
        dateTo,
        type,
        value,
        userId
    );
  }
}
