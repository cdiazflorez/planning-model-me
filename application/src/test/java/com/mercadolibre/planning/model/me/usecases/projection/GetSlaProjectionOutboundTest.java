package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link GetSlaProjectionOutbound}.
 */
@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionOutboundTest {

  private static final List<String> STATUSES = of("pending", "to_route", "to_pick", "picked", "to_sort",
      "sorted", "to_group", "grouping", "grouped", "to_pack");

  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

  private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);

  private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);

  private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);

  private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);

  @InjectMocks
  private GetSlaProjectionOutbound getSlaProjectionOutbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private FeatureSwitches featureSwitches;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Test
  void testExecuteWithError() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime;
    final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusDays(4);
    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(utcDateTimeFrom)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    final List<ProcessName> processes = of(PICKING, PACKING, PACKING_WALL);

    when(planningModelGateway.runProjection(
        createProjectionRequestOutbound(mockedBacklog, processes, utcDateTimeFrom, utcDateTimeTo)))
        .thenThrow(RuntimeException.class);

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        of("outbound-orders"),
        STATUSES,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(1).plusHours(1).toInstant(),
        of("date_out"))
    ).thenReturn(of(
        new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
        new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
        new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true),
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)));

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getData());
  }

  private ProjectionRequest createProjectionRequestOutbound(final List<Backlog> backlogs,
                                                            final List<ProcessName> processes,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo) {
    return ProjectionRequest.builder()
        .processName(processes)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .type(ProjectionType.CPT)
        .backlog(backlogs)
        .applyDeviation(true)
        .timeZone(BA_ZONE)
        .build();
  }

  private List<Backlog> mockBacklog() {
    return of(
        new Backlog(CPT_1, 150),
        new Backlog(CPT_2, 235),
        new Backlog(CPT_3, 300),
        new Backlog(CPT_4, 120)
    );
  }
}
