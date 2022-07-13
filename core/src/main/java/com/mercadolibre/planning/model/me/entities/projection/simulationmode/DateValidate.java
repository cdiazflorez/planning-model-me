package com.mercadolibre.planning.model.me.entities.projection.simulationmode;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class DateValidate {

    ZonedDateTime date;

    @JsonProperty("is_valid")
    boolean valid;
}
