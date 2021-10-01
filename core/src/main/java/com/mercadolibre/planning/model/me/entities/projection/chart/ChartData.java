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
    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");

    private String title;
    private String cpt;
    private String projectedEndTime;
    private ProcessingTime processingTime;
    private Boolean isDeferred;
    private ChartTooltip tooltip;

    public static ChartData fromProjection(final ZonedDateTime cpt,
                                           final ZonedDateTime projectedEndDate,
                                           final ZonedDateTime dateTo,
                                           final int remainingQuantity,
                                           final ProcessingTime processingTime,
                                           final boolean isDeferred) {

        String projectEnd = projectedEndDate == null
                ? dateTo.format(DATE_FORMATTER)
                : projectedEndDate.format(DATE_FORMATTER);

        return ChartData.builder()
                .title(cpt.format(DATE_SHORT_FORMATTER))
                .cpt(cpt.format(DATE_FORMATTER))
                .projectedEndTime(projectEnd)
                .processingTime(processingTime)
                .tooltip(createChartTooltip(
                        cpt, projectedEndDate, dateTo, remainingQuantity,
                        processingTime.getValue(), isDeferred)
                )
                .isDeferred(isDeferred)
                .build();
    }
}
