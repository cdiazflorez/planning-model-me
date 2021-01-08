package com.mercadolibre.planning.model.me.entities.projection.chart;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.mercadolibre.planning.model.me.entities.projection.chart.ChartTooltip.createChartTooltip;
import static java.time.format.DateTimeFormatter.ofPattern;

@Value
@Builder
public class ChartData {

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:mm");

    private String title;
    private String cpt;
    private String projectedEndTime;
    private ChartTooltip tooltip;

    public static ChartData fromProjection(final ZonedDateTime cpt,
                                           final ZonedDateTime projectedEndDate,
                                           final ZonedDateTime dateTo,
                                           final int remainingQuantity) {


        return ChartData.builder()
                .title(cpt.format(HOUR_FORMAT))
                .cpt(cpt.format(DATE_FORMATTER))
                .projectedEndTime(projectedEndDate.format(DATE_FORMATTER))
                .tooltip(createChartTooltip(cpt, projectedEndDate, dateTo, remainingQuantity))
                .build();
    }
}
