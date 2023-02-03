package com.mercadolibre.planning.model.me.usecases.deviation.dtos;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class SaveDeviationInput {

  String warehouseId;

  Workflow workflow;

  List<ShipmentType> paths;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  DeviationType type;

  Double value;

  long userId;

}
