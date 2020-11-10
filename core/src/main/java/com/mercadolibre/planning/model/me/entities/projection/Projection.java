package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import lombok.Value;

@Value
public class Projection {

    private String title;

    @JsonProperty("complex_table_1")
    private ComplexTable complexTable1;

    @JsonProperty("simple_table_2")
    private SimpleTable simpleTable2;

    private Chart chart;

}
