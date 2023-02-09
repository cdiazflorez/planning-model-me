package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class SaveDeviationRequest {

    String logisticCenterId;
    Workflow workflow;
    List<ShipmentType> affectedShipmentTypes;
    ZonedDateTime dateFrom;
    ZonedDateTime dateTo;
    DeviationType type;
    Double value;
    Long userId;
}
