package com.mercadolibre.planning.model.me.entities.simulation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.entities.Result;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class SimulationResult extends Result {
    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime simulatedEndDate;
}
