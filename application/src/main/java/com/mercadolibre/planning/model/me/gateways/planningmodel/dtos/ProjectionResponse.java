package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ProjectionResponse {

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime date;

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;
}
