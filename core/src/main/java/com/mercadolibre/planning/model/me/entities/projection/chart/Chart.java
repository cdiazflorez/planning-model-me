package com.mercadolibre.planning.model.me.entities.projection.chart;

import lombok.Value;

import java.util.List;

@Value
public class Chart {
    private ProcessingTime processingTime;
    private List<ChartData> data;
}
