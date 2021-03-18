package com.mercadolibre.planning.model.me.metric;

import com.mercadolibre.metrics.MetricCollector;
import org.springframework.stereotype.Component;

import static com.mercadolibre.metrics.Metrics.INSTANCE;

@Component
public class DatadogWrapper {
    public void incrementCounter(final String name, final MetricCollector.Tags values) {
        INSTANCE.incrementCounter(name, values.toArray());
    }
}
