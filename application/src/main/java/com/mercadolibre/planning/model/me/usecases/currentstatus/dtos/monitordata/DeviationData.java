package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.MonitorDataType.DEVIATION;

import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.deviation.DeviationMetric;

@Getter
@Builder
@AllArgsConstructor
public class DeviationData extends MonitorData {

    private final DeviationMetric metrics;
    
    public DeviationData() {
        this.type = DEVIATION.getType();
        metrics=DeviationMetric.builder().build();
    }

}
