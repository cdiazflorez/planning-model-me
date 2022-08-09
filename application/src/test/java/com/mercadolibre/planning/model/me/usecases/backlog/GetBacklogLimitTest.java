package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import com.mercadolibre.planning.model.me.utils.TestException;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    assertEquals(0, waving.get(A_DATE.toInstant()).getMin());
    assertEquals(15, waving.get(A_DATE.toInstant()).getMax());

    assertEquals(-1, waving.get(ANOTHER_DATE.toInstant()).getMin());
    assertEquals(-1, waving.get(ANOTHER_DATE.toInstant()).getMax());
  }

  @Test
  void testSearchEntitiesError() {
    // GIVEN
    when(planningModelGateway.searchTrajectories(any(SearchTrajectoriesRequest.class)))
        .thenThrow(new TestException());

    final var input = input();

    // WHEN
    assertThrows(
        TestException.class,
        () -> getBacklogLimits.execute(input)
    );
  }

  private GetBacklogLimitsInput input() {
    return GetBacklogLimitsInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .processes(of(WAVING, PICKING, PACKING))
        .dateFrom(A_DATE.toInstant())
        .dateTo(ANOTHER_DATE.toInstant())
        .build();
  }

  void mockPlanningResponse() {
    final var input = SearchTrajectoriesRequest.builder()
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .entityTypes(of(BACKLOG_LOWER_LIMIT, BACKLOG_UPPER_LIMIT))
        .warehouseId(WAREHOUSE_ID)
        .processName(of(WAVING, PICKING, PACKING))
        .dateFrom(A_DATE)
        .dateTo(ANOTHER_DATE)
        .build();

    when(planningModelGateway.searchTrajectories(input)).thenReturn(
        Map.of(
            BACKLOG_LOWER_LIMIT, of(
                mvp(A_DATE, WAVING, 0),
                mvp(A_DATE, PICKING, 1),
                mvp(A_DATE, PACKING, 2),
                mvp(ANOTHER_DATE, WAVING, -1),
                mvp(ANOTHER_DATE, PICKING, -1),
                mvp(ANOTHER_DATE, PACKING, -1)
            ),
            BACKLOG_UPPER_LIMIT, of(
                mvp(A_DATE, WAVING, 15),
                mvp(A_DATE, PICKING, 10),
                mvp(A_DATE, PACKING, 5),
                mvp(ANOTHER_DATE, WAVING, -1),
                mvp(ANOTHER_DATE, PICKING, -1),
                mvp(ANOTHER_DATE, PACKING, -1)
            )
        )
    );
  }

  private MagnitudePhoto mvp(ZonedDateTime date, ProcessName process, Integer value) {
    return MagnitudePhoto.builder()
        .date(date)
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .processName(process)
        .value(value)
        .source(Source.FORECAST)
        .metricUnit(MetricUnit.MINUTES)
        .build();
  }
}
