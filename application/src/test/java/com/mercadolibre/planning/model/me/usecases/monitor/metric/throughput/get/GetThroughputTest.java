package com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.get;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.GetThroughput;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.throughput.ThroughputInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.THROUGHPUT_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class GetThroughputTest {

    @InjectMocks
    private GetThroughput getThroughput;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final ThroughputInput input = ThroughputInput.builder()
                .processInfo(PACKING)
                .processedUnitLastHour(UnitsResume.builder()
                        .process(AnalyticsQueryEvent.PACKING_NO_WALL)
                        .eventCount(2020)
                        .unitCount(3020)
                        .build())
                .build();
        // WHEN
        Metric metric = getThroughput.execute(input);

        // THEN
        assertEquals(THROUGHPUT_PER_HOUR.getSubtitle(), metric.getSubtitle());
        assertEquals(THROUGHPUT_PER_HOUR.getTitle(), metric.getTitle());
        assertEquals(THROUGHPUT_PER_HOUR.getType(), metric.getType());
        assertEquals("3020 uds./h", metric.getValue());
    }

}
