package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Map;

@Value
public class Content {

    private String title;

    private ZonedDateTime date;

    private Map<String, String> tooltip;

    private String id;

    private boolean isValid;
}
