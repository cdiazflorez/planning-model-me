package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationActions;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.deviation.DeviationMetric;
import lombok.Getter;

@Getter
public class DeviationData extends MonitorData {

    private final DeviationActions actions;
    private final DeviationMetric metrics;

    public DeviationData() {
        this.type = MonitorDataType.DEVIATION.getType();
        metrics = DeviationMetric.builder().build();
        actions = DeviationActions.builder().build();
    }

    public DeviationData(DeviationMetric metrics,
                         DeviationActions actions) {
        this.type = MonitorDataType.DEVIATION.getType();
        this.metrics = metrics;
        this.actions = actions;
    }

}
