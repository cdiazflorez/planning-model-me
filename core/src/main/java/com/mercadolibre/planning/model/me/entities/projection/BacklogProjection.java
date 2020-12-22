package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BacklogProjection {

    String title;

    List<Tab> tabs;

    Selections selections;

    @JsonProperty("simple_table_1")
    SimpleTable simpleTable1;

    @JsonProperty("simple_table_2")
    SimpleTable simpleTable2;
}
