package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import lombok.Value;

import java.util.List;

@Value
public class Projection {

    private String title;

    @JsonProperty("simple_table_1")
    private SimpleTable simpleTable1;

    @JsonProperty("complex_table_1")
    private ComplexTable complexTable1;

    @JsonProperty("simple_table_2")
    private SimpleTable simpleTable2;

    private Chart chart;

    List<Tab> tabs;

}
