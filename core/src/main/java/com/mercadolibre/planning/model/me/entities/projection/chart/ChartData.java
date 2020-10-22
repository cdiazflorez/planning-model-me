package com.mercadolibre.planning.model.me.entities.projection.chart;

import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import lombok.Value;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

@Value
public class ChartData {
    private String title;
    private String cpt;
    private String projectedEndTime;

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:mm");

    public static ChartData fromProjectionResponse(final ProjectionResult projectionResult,
                                                   final ZoneId zoneId) {
        return new ChartData(
                projectionResult.getDate().toLocalDateTime().atZone(zoneId)
                        .format(HOUR_FORMAT.withZone(zoneId)),
                projectionResult.getDate().toLocalDateTime().atZone(zoneId).format(DATE_FORMATTER),
                projectionResult.getProjectedEndDate().toLocalDateTime().atZone(zoneId)
                        .format(DATE_FORMATTER.withZone(zoneId))
        );
    }
}
