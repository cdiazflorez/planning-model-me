package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
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
public class ProjectionResult {
    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime date;

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime projectedEndDate;

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime simulatedEndDate;

    private int remainingQuantity;

    private ProcessingTime processingTime;

    private boolean isDeferred;
}
