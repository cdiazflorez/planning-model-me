package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MetricType {
    BACKLOG("Backlog en","backlog", null),
    THROUGHPUT_PER_HOUR("Procesamiento", "throughput_per_hour", "última hora"),
    PRODUCTIVITY("Productividad", "productivity", "por persona")
    ;

    private final String title;
    private final String type;
    private final String subtitle;
}
