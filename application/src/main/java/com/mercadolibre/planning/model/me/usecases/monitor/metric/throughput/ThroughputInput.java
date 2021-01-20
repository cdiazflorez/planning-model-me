package com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput;

import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ThroughputInput {

    private final ProcessInfo processInfo;
    private final UnitsResume processedUnitLastHour;

}
