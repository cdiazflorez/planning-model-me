package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomInstantDeserializer;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/** The consolidation of a group of cells of a backlog photo. */
@Value
public class Consolidation {

    /**
     * The instant when the backlog photo was taken.
     */
    @JsonDeserialize(using = CustomInstantDeserializer.class)
    private Instant date;

    /**
     * The fields by which the backlog photo cells were grouped and the values of those fields
     * corresponding to the group.
     */
    private Map<String, String> keys;

    /**
     * The sum of the quantity of all the cells of the group.
     */
    private Integer total;
}
