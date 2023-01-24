package com.mercadolibre.planning.model.me.usecases.deviation.dtos;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SaveDeviationInput {

  private Workflow workflow;
  private String warehouseId;
  private ZonedDateTime dateFrom;
  private ZonedDateTime dateTo;
  private DeviationType type;
  private Double value;
  private long userId;
  private List<ShipmentType> shipmentTypes;

}
