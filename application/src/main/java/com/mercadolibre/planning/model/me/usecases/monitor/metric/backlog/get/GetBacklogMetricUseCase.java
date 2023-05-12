package com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric.createMetric;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.GetMetric;
import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
