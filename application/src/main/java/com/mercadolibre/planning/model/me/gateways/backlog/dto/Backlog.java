package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Map;

@Value
public class Backlog {

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    private ZonedDateTime date;

    private Map<String, String> keys;

    private Integer total;
}
