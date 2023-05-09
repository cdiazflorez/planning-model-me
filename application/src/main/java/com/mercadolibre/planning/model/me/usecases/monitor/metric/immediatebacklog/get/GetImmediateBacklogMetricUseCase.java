package com.mercadolibre.planning.model.me.usecases.monitor.metric.immediatebacklog.get;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createMetric;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetImmediateBacklogMetricUseCase implements GetMetric<BacklogMetricInput, Metric> {

    @Override
    public Metric execute(BacklogMetricInput input) {
        final String quantity = NumberFormat.getNumberInstance(Locale.GERMAN)
                .format(input.getQuantity());
        return createMetric(input.getProcessOutbound(), quantity + " uds.", IMMEDIATE_BACKLOG);
    }

}
