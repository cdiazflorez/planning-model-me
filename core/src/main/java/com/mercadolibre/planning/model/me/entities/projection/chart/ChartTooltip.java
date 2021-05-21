package com.mercadolibre.planning.model.me.entities.projection.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

@Value
@Builder
public class ChartTooltip {

    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("HH:mm");

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
        final String subtitle3 =  projectedEndDate.isEqual(dateTo)
                ? "Excede las 24hs" : projectedEndDate.format(HOUR_FORMAT);

        ChartTooltipBuilder chartTooltipBuilder = ChartTooltip.builder()
                .title1("CPT:")
                .subtitle1(cpt.format(HOUR_FORMAT))
                .title2("Desviación:")
                .subtitle2(subtitle2)
                .title3("Cierre proyectado:")
                .subtitle3(subtitle3)
                .title4("Cycle time:")
                .subtitle4(Duration.ofMinutes(processingTime).toHours() + " horas");

        if (isDeferred) {
            chartTooltipBuilder.title5("Diferido");
        }

        return chartTooltipBuilder.build();
    }
}
