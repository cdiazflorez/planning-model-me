package com.mercadolibre.planning.model.me.metric;

import static com.mercadolibre.metrics.Metrics.INSTANCE;

import com.mercadolibre.metrics.MetricCollector;
import org.springframework.stereotype.Component;

@Component
public class DatadogWrapper {
    public void incrementCounter(final String name, final MetricCollector.Tags values) {
        INSTANCE.incrementCounter(name, values.toArray());
    }
}
