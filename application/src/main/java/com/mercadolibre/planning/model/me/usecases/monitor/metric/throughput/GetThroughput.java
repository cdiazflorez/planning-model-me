package com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createEmptyMetric;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createMetric;

@Slf4j
@Named
@AllArgsConstructor
public class GetThroughput implements GetMetric<ThroughputInput, Metric> {

    @Override
    public Metric execute(ThroughputInput input) {
        if (input.getProcessedUnitLastHour() == null) {
            return createEmptyMetric(THROUGHPUT_PER_HOUR, input.getProcessOutbound());
        }
        return createMetric(input.getProcessOutbound(),
                input.getProcessedUnitLastHour().getUnitCount() + " uds./h",
                THROUGHPUT_PER_HOUR);
    }

}
