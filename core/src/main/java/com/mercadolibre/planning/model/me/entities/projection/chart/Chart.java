package com.mercadolibre.planning.model.me.entities.projection.chart;

import java.util.List;
import lombok.Value;

@Value
public class Chart {
    private List<ChartData> data;
}
