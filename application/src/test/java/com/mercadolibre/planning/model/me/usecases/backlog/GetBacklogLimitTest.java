package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.utils.TestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBacklogLimitTest {

    private static final ZonedDateTime ANOTHER_DATE = A_DATE.plusHours(1L);

    @InjectMocks
    private GetBacklogLimits getBacklogLimits;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Test
    void testExecuteOk() {
        // GIVEN
        mockPlanningResponse();

        final var input = input();

        // WHEN
        final var result = getBacklogLimits.execute(input);

        // THEN
        final var waving = result.get(WAVING);

        assertEquals(0, waving.get(A_DATE).getMin());
        assertEquals(15, waving.get(A_DATE).getMax());

        assertEquals(-1, waving.get(ANOTHER_DATE).getMin());
        assertEquals(-1, waving.get(ANOTHER_DATE).getMax());
    }


    @Test
    void testSearchEntitiesError() {
        // GIVEN
        when(planningModelGateway.searchEntities(any(SearchEntitiesRequest.class)))
                .thenThrow(new TestException());

        final var input = input();

        // WHEN
        assertThrows(
                TestException.class,
                () -> getBacklogLimits.execute(input)
        );
    }

    private GetBacklogLimitsInput input() {
        return  GetBacklogLimitsInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .processes(List.of(WAVING, PICKING, PACKING))
                .dateFrom(A_DATE)
                .dateTo(ANOTHER_DATE)
                .build();
    }

    void mockPlanningResponse() {
        final var input = SearchEntitiesRequest.builder()
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .entityTypes(of(BACKLOG_LOWER_LIMIT, BACKLOG_UPPER_LIMIT))
                .warehouseId(WAREHOUSE_ID)
                .processName(of(WAVING, PICKING, PACKING))
                .dateFrom(A_DATE)
                .dateTo(ANOTHER_DATE)
                .build();

        when(planningModelGateway.searchEntities(input)).thenReturn(
                Map.of(
                        BACKLOG_LOWER_LIMIT, of(
                                entity(A_DATE, WAVING, 0),
                                entity(A_DATE, PICKING, 1),
                                entity(A_DATE, PACKING, 2),
                                entity(ANOTHER_DATE, WAVING, -1),
                                entity(ANOTHER_DATE, PICKING, -1),
                                entity(ANOTHER_DATE, PACKING, -1)
                        ),
                    BACKLOG_UPPER_LIMIT, of(
                                entity(A_DATE, WAVING, 15),
                                entity(A_DATE, PICKING, 10),
                                entity(A_DATE, PACKING, 5),
                                entity(ANOTHER_DATE, WAVING, -1),
                                entity(ANOTHER_DATE, PICKING, -1),
                                entity(ANOTHER_DATE, PACKING, -1)
                        )
                )
        );
    }

    private Entity entity(ZonedDateTime date, ProcessName process, Integer value) {
        return Entity.builder()
                .date(date)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .processName(process)
                .value(value)
                .source(Source.FORECAST)
                .metricUnit(MetricUnit.MINUTES)
                .build();
    }
}
