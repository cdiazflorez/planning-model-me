package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.Value;

@Value
public class Content {

    private String title;

    private ZonedDateTime date;

    private Map<String, String> tooltip;

    private String id;

    @JsonProperty("is_valid")
    private boolean valid;
}
