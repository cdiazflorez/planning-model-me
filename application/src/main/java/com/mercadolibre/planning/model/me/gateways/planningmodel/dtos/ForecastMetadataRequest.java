package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class ForecastMetadataRequest {

    private String warehouseId;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

}
