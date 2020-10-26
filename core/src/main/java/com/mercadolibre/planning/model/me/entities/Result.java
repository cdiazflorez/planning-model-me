package com.mercadolibre.planning.model.me.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Result {
    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime date;

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;
}
