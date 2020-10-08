package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Projection {

    private String title;

    @JsonProperty("complex_table_1")
    private ComplexTable complexTable1;

}
