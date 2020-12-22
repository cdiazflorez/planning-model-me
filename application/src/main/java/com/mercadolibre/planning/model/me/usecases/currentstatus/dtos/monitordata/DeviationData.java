package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation.DeviationMetric;

import lombok.Builder;
import lombok.Getter;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorDataType.DEVIATION;

@Getter
@Builder
public class DeviationData extends MonitorData {

    private final DeviationMetric metrics;
    
    public DeviationData() {
        this.type = DEVIATION.getType();
        metrics = DeviationMetric.builder().build();
    }
    
    public DeviationData(DeviationMetric metrics) {
        this.type = DEVIATION.getType();
        this.metrics = metrics;
    }

}
