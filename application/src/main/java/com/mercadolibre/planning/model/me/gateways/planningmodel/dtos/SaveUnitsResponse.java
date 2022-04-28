package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Contains the response parameters after saving Units Distribution. */
@Builder
@Getter
@Setter
public class SaveUnitsResponse {

  String response;

  Integer quantitySave;

  String warehouseId;
}
