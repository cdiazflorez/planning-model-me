package com.mercadolibre.planning.model.me.usecases.monitor.metric;

import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;

public interface GetMetric<T, P> extends UseCase<T, P> {

    static Metric createEmptyMetric(final MetricType metricType, final ProcessOutbound process) {
        return createMetric(process, "-", metricType);
    }

    static Metric createMetric(final ProcessOutbound process,
                                final String value,
                                final MetricType metricType) {
        return Metric.builder()
                .title(metricType.getTitle())
                .type(metricType.getType())
                .subtitle(TOTAL_BACKLOG == metricType ||  IMMEDIATE_BACKLOG == metricType
                        ? process.getSubtitle() : metricType.getSubtitle())
                .value(value)
                .build();
    }
}
