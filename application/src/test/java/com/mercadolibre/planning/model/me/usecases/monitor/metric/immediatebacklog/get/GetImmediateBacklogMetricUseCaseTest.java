package com.mercadolibre.planning.model.me.usecases.monitor.metric.immediatebacklog.get;

import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.backlog.get.BacklogMetricInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.IMMEDIATE_BACKLOG;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class GetImmediateBacklogMetricUseCaseTest {

    @InjectMocks
    private GetImmediateBacklogMetricUseCase getImmediateBacklogMetricUseCase;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final BacklogMetricInput input = BacklogMetricInput.builder()
                .processInfo(OUTBOUND_PLANNING)
                .quantity(10)
                .build();

        // WHEN
        Metric metric = getImmediateBacklogMetricUseCase.execute(input);

        // THEN
        assertEquals(OUTBOUND_PLANNING.getSubtitle(), metric.getSubtitle());
        assertEquals(IMMEDIATE_BACKLOG.getTitle(), metric.getTitle());
        assertEquals(IMMEDIATE_BACKLOG.getType(), metric.getType());
        assertEquals("10 uds.", metric.getValue());
    }
}
