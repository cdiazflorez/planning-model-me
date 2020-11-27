package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class ForecastMetadataRequest {

    private String warehouseId;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

}
