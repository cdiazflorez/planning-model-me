package com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.text.NumberFormat;
import java.util.Locale;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createMetric;

@Slf4j
@Named
@AllArgsConstructor
public class GetBacklogMetricUseCase implements GetMetric<BacklogMetricInput, Metric> {

    @Override
    public Metric execute(BacklogMetricInput input) {
        final String quantity = NumberFormat.getNumberInstance(Locale.GERMAN)
                .format(input.getQuantity());
        return createMetric(input.getProcessOutbound(), quantity + " uds.", TOTAL_BACKLOG);
    }

}
