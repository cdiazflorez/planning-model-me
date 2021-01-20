package com.mercadolibre.planning.model.me.usecases.monitor.metric;

import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;

public interface GetMetric<T, P> extends UseCase<T, P> {

    static Metric createEmptyMetric(final MetricType metricType, final ProcessInfo process) {
        return createMetric(process, "-", metricType);
    }

    static Metric createMetric(final ProcessInfo process,
                                final String value,
                                final MetricType metricType) {
        return Metric.builder()
                .title(metricType.getTitle())
                .type(metricType.getType())
                .subtitle(metricType == MetricType.BACKLOG
                        ? process.getSubtitle() : metricType.getSubtitle())
                .value(value)
                .build();
    }
}
