package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MetricType {
    TOTAL_BACKLOG("Total", MagnitudeType.BACKLOG.getName(), null),
    IMMEDIATE_BACKLOG("Inmediato", MagnitudeType.BACKLOG.getName(), null),
    THROUGHPUT_PER_HOUR("Procesamiento", "throughput_per_hour", "última hora"),
    PRODUCTIVITY("Productividad", "productivity", "por persona");

    private final String title;
    private final String type;
    private final String subtitle;
}
