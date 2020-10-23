package com.mercadolibre.planning.model.me.entities.projection.chart;

import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import lombok.Value;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.time.format.DateTimeFormatter.ofPattern;

@Value
public class ChartData {

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:mm");

    private String title;
    private String cpt;
    private String projectedEndTime;

    public static ChartData fromProjectionResponse(final ProjectionResult projectionResult,
                                                   final ZoneId zoneId,
                                                   final ZonedDateTime utcDateTo) {
        final ZonedDateTime cpt = convertToTimeZone(zoneId, projectionResult.getDate());
        final ZonedDateTime dateTo = convertToTimeZone(zoneId, utcDateTo);
        final ZonedDateTime projectedEndDate = convertToTimeZone(zoneId,
                projectionResult.getProjectedEndDate());

        return new ChartData(
                cpt.format(HOUR_FORMAT),
                cpt.format(DATE_FORMATTER),
                getDefaultProjectedEndTime(projectedEndDate, dateTo)
        );
    }

    private static String getDefaultProjectedEndTime(final ZonedDateTime projectEndDate,
                                                     final ZonedDateTime dateTo) {

        return  projectEndDate == null
                ? dateTo.format(DATE_FORMATTER)
                : projectEndDate.format(DATE_FORMATTER);
    }
}
