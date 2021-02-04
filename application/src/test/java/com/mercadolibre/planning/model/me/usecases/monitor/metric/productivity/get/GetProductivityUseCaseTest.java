package com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.get;

import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.usecases.monitor.currentstatus.get.GetCurrentStatusInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.Metric;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.GetProductivity;
import com.mercadolibre.planning.model.me.usecases.monitor.metric.productivity.ProductivityInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.MetricType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProductivityUseCaseTest {

    @InjectMocks
    private GetProductivity getProductivityUseCaseTest;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Test
    public void testExecuteOk() {
        // GIVEN
        final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
        final GetCurrentStatusInput input = GetCurrentStatusInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(utcCurrentTime)
                .dateTo(utcCurrentTime.plusHours(25))
                .build();

        final ProductivityInput productivityInput = ProductivityInput.builder()
                .processInfo(PACKING)
                .monitorInput(input)
                .processedUnitLastHour(UnitsResume.builder()
                        .process(AnalyticsQueryEvent.PICKING)
                        .eventCount(2020)
                        .unitCount(3020)
                        .build()
                )
                .headcounts(mockHeadcountEntities(utcCurrentTime))
                .build();

        // WHEN
        Metric metric = getProductivityUseCaseTest.execute(productivityInput);

        // THEN
        assertEquals(PRODUCTIVITY.getSubtitle(), metric.getSubtitle());
        assertEquals(PRODUCTIVITY.getTitle(), metric.getTitle());
        assertEquals(PRODUCTIVITY.getType(), metric.getType());
        assertNotEquals("0 uds./h",metric.getValue());
    }

    private List<Entity> mockHeadcountEntities(final ZonedDateTime utcCurrentTime) {
        return List.of(
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(10)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime)
                        .processName(PICKING)
                        .source(Source.SIMULATION)
                        .value(20)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusHours(2))
                        .processName(ProcessName.PACKING)
                        .source(Source.FORECAST)
                        .value(15)
                        .build(),
                Entity.builder()
                        .date(utcCurrentTime.plusDays(1))
                        .processName(PICKING)
                        .source(Source.FORECAST)
                        .value(30)
                        .build()
        );
    }
}
