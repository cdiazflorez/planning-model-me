package com.mercadolibre.planning.model.me.entities.projection.chart;

import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class ChartData {
    private String title;
    private String cpt;
    private ZonedDateTime projectedEndTime;

    public static ChartData fromProjectionResponse(final ProjectionResult projectionResult) {
        return new ChartData(
                String.valueOf(projectionResult.getDate().getHour()),
                projectionResult.getDate().toString(),
                projectionResult.getProjectedEndDate()
        );
    }
}
