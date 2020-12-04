package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MetricType {
    BACKLOG("Backlog en","backlog"),
    THROUGHPUT_PER_HOUR("Procesamiento", "throughput_per_hour"),
    PRODUCTIVITY("Productividad", "productivity")
    ;

    private final String title;
    private final String type;
}
