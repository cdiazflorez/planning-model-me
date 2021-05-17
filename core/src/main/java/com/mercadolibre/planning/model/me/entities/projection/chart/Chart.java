package com.mercadolibre.planning.model.me.entities.projection.chart;

import lombok.Value;

import java.util.List;

@Value
public class Chart {
    private List<ChartData> data;
}
