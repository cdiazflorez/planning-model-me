package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class ProjectionValue {

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    ZonedDateTime date;

    int quantity;

    String source;
}
