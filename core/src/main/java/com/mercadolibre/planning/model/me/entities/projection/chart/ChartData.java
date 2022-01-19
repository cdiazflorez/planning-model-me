package com.mercadolibre.planning.model.me.entities.projection.chart;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.mercadolibre.planning.model.me.entities.projection.chart.ChartTooltip.createChartTooltip;
import static com.mercadolibre.planning.model.me.entities.projection.chart.ChartTooltip.createChartTooltipInbound;
import static java.time.format.DateTimeFormatter.ofPattern;

@Value
@Builder
public class ChartData {

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter DATE_SHORT_FORMATTER = ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = ofPattern("dd/MM");

    private String title;
    private String cpt;
    private String projectedEndTime;
    private ProcessingTime processingTime;
    private Boolean isDeferred;
    private Boolean isExpired;
    private ChartTooltip tooltip;

    public static ChartData fromProjection(final ZonedDateTime cpt,
                                           final ZonedDateTime projectedEndDate,
                                           final ZonedDateTime dateTo,
                                           final int remainingQuantity,
                                           final ProcessingTime processingTime,
                                           final boolean isDeferred,
                                           final boolean isExpired) {

        final String title = cpt.toInstant().isAfter(Instant.now())
                ? cpt.format(DATE_SHORT_FORMATTER)
                : cpt.format(DATE_ONLY_FORMATTER);

        return ChartData.builder()
                .title(title)
                .cpt(cpt.format(DATE_FORMATTER))
                .projectedEndTime(projectEndDate(projectedEndDate, dateTo))
                .processingTime(processingTime)
                .tooltip(createChartTooltip(
                        cpt, projectedEndDate, dateTo, remainingQuantity,
                        processingTime.getValue(), isDeferred)
                )
                .isDeferred(isDeferred)
                .isExpired(isExpired)
                .build();
    }

    public static ChartData fromProjectionInbound(final ZonedDateTime cpt,
                                 final ZonedDateTime projectedEndDate,
                                 final ZonedDateTime dateTo,
                                 final int remainingQuantity,
                                 final ProcessingTime processingTime,
                                 final boolean isDeferred,
                                 final boolean isExpired) {

        final String title = isExpired
                ? cpt.format(DATE_ONLY_FORMATTER)
                : cpt.format(DATE_SHORT_FORMATTER);

        return ChartData.builder()
                .title(title)
                .cpt(cpt.format(DATE_FORMATTER))
                .projectedEndTime(projectEndDate(projectedEndDate, dateTo))
                .processingTime(processingTime)
                .tooltip(createChartTooltipInbound(cpt, projectedEndDate, remainingQuantity))
                .isDeferred(isDeferred)
                .isExpired(isExpired)
                .build();
    }

    private static String projectEndDate(final ZonedDateTime projectedEndDate, final ZonedDateTime dateTo) {
        return projectedEndDate == null
                ? dateTo.format(DATE_FORMATTER)
                : projectedEndDate.format(DATE_FORMATTER);
    }
}
