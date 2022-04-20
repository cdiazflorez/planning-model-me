package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.*;


@Builder
@Getter
@Setter
public class SaveUnitsResponse {

     String response;
     Integer quantitySave;
     String warehouseId;
}
