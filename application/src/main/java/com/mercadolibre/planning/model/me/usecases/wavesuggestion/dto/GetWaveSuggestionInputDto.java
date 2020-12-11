package com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@SuperBuilder
@Value
public class GetWaveSuggestionInputDto {

    ZoneId zoneId;
    Workflow workflow;
    String warehouseId;
}
