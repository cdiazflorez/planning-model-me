package com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.TOTAL_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetBacklogMetricUseCaseTest {

    @InjectMocks
    private GetBacklogMetricUseCase getBacklogMetricUseCase;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final BacklogMetricInput input = BacklogMetricInput.builder()
                .processOutbound(PACKING)
                .quantity(10)
                .build();

        // WHEN
        Metric metric = getBacklogMetricUseCase.execute(input);

        // THEN
        assertEquals(PACKING.getSubtitle(), metric.getSubtitle());
        assertEquals(TOTAL_BACKLOG.getTitle(), metric.getTitle());
        assertEquals(TOTAL_BACKLOG.getType(), metric.getType());
        assertEquals("10 uds.", metric.getValue());
    }
}
