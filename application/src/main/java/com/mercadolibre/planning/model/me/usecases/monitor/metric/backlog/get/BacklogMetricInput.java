package com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class BacklogMetricInput {

    private final int quantity;
    private final ProcessInfo processInfo;

}
