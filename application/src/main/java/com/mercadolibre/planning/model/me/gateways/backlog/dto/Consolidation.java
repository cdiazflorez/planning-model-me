package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomInstantDeserializer;
import java.time.Instant;
import java.util.Map;
import lombok.Value;

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
    private int total;

    /**
     * The value of this field is `true` when the photo from which this consolidation was created is the first that was
     * taken in the period of the time partition (currently the partition is by hour).
     */
    private boolean firstPhotoOfPeriod;
}
