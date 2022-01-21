package com.mercadolibre.planning.model.me.entities.projection.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

@Value
@Builder
public class ChartTooltip {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:mm");

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd/MM");

    private static final DateTimeFormatter DATE_HOUR_FORMAT = ofPattern("dd/MM - HH:mm");

    private static final String OVERDUE_TAG = " - Vencidos";

    private static final String UNITS_TAG = " uds.";

    @JsonProperty("title_1")
    private String title1;

    @JsonProperty("subtitle_1")
    private String subtitle1;

    @JsonProperty("title_2")
    private String title2;

    @JsonProperty("subtitle_2")
    private String subtitle2;

    @JsonProperty("title_3")
    private String title3;

    @JsonProperty("subtitle_3")
    private String subtitle3;

    @JsonProperty("title_4")
    private String title4;

    @JsonProperty("subtitle_4")
    private String subtitle4;

    @JsonProperty("title_5")
    private String title5;

    public static ChartTooltip createChartTooltip(final ZonedDateTime cpt,
                                                  final ZonedDateTime projectedEndDate,
                                                  final ZonedDateTime dateTo,
                                                  final int remainingQuantity,
                                                  final long processingTime,
                                                  final boolean isDeferred) {
        final String subtitle2 = remainingQuantity == 0 ? "-" : String.valueOf(remainingQuantity);
        final String subtitle3 = projectedEndDate == null
                ? "Excede las 24hs" : projectedEndDate.format(DATE_HOUR_FORMAT);

        ChartTooltipBuilder chartTooltipBuilder = ChartTooltip.builder()
                .title1("CPT:")
                .subtitle1(cpt.format(HOUR_FORMAT))
                .title2("Desviación:")
                .subtitle2(subtitle2)
                .title3("Cierre proyectado:")
                .subtitle3(subtitle3)
                .title4("Cycle time:")
                .subtitle4(createProcessingTimeLabel(processingTime));

        if (isDeferred) {
            chartTooltipBuilder.title5("Diferido");
        }

        return chartTooltipBuilder.build();
    }

    public static ChartTooltip createChartTooltipInbound(final ZonedDateTime sla,
                                                         final ZonedDateTime projectedEndDate,
                                                         final int remainingQuantity) {

        final String subtitle2 = remainingQuantity == 0
                ? "-"
                : String.valueOf(remainingQuantity) + UNITS_TAG;


        final String subtitle3 = projectedEndDate == null
                ? "Excede las 24hs" : projectedEndDate.format(DATE_HOUR_FORMAT);

        final String title = sla.toInstant().isAfter(Instant.now())
                ? sla.format(HOUR_FORMAT)
                : sla.format(DATE_FORMAT) + OVERDUE_TAG;

        ChartTooltipBuilder chartTooltipBuilder = ChartTooltip.builder()
                .title1("SLA:")
                .subtitle1(title)
                .title2("Desviación:")
                .subtitle2(subtitle2)
                .title3("Cierre proyectado:")
                .subtitle3(subtitle3);

        return chartTooltipBuilder.build();
    }

    private static String createProcessingTimeLabel(final long processingTime) {
        final long hours = Duration.ofMinutes(processingTime).toHours();
        final long minutes = processingTime % 60;

        final StringBuilder label = new StringBuilder();

        if (hours != 0) {
            label.append(hours);
            label.append(" horas");

            if (minutes != 0) {
                label.append(" y ");
                label.append(minutes);
                label.append(" minutos");
            }
        } else if (minutes != 0) {
            label.append(minutes);
            label.append(" minutos");
        } else {
            label.append("-");
        }
        return label.toString();
    }
}
