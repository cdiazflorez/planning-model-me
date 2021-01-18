package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviationData extends MonitorData {

    private final DeviationMetric metrics;
    
    public DeviationData() {
        this.type = MonitorDataType.DEVIATION.getType();
        metrics = DeviationMetric.builder().build();
    }
    
    public DeviationData(DeviationMetric metrics) {
        this.type = MonitorDataType.DEVIATION.getType();
        this.metrics = metrics;
    }

}
