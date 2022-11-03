package com.mercadolibre.planning.model.me.services.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BacklogApiAdapterTest {

  private static final ZonedDateTime NOW = ZonedDateTime.now();

  @InjectMocks
  private BacklogApiAdapter backlogApiAdapter;

  @Mock
  private BacklogApiGateway backlogApiGateway;

  @Mock
  private ProjectBacklog backlogProjection;

  @Test
  void testExecuteCurrentBacklog() {
    // GIVEN
    final List<Consolidation> consolidations = getConsolidation();

    final BacklogRequest gatewayRequest = new BacklogRequest(
        Instant.from(NOW),
        "ARBA01",
        of("outbound-orders"),
        of(WAVING.getName(), PICKING.getName(), PACKING.getName()),
        emptyList(),
        of("date_out"),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW.plusHours(24))
    );

    // WHEN
    when(backlogApiGateway.getBacklog(gatewayRequest)).thenReturn(consolidations);

    final List<Consolidation> result = backlogApiAdapter.getCurrentBacklog(
        Instant.from(NOW),
        "ARBA01",
        of(FBM_WMS_OUTBOUND),
        of(WAVING, PICKING, PACKING),
        of(DATE_OUT),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW.plusHours(24))
    );

    // THEN
    assertEquals(consolidations.get(0).getDate(), result.get(0).getDate());
    assertEquals(consolidations.get(0).getTotal(), result.get(0).getTotal());
    assertEquals(consolidations.get(0).getKeys(), result.get(0).getKeys());
  }

  @Test
  void testExecuteCurrentBacklogInbound() {
    // GIVEN
    final List<Consolidation> consolidations = getConsolidation();

    final BacklogRequest gatewayRequest = new BacklogRequest(
        Instant.from(NOW),
        "ARBA01",
        of("inbound", "inbound-transfer"),
        of(CHECK_IN.getName(), PUT_AWAY.getName()),
        emptyList(),
        of("date_out"),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW.plusHours(24))
    );

    // WHEN
    when(backlogApiGateway.getBacklog(gatewayRequest)).thenReturn(consolidations);

    final List<Consolidation> result = backlogApiAdapter.getCurrentBacklog(
        Instant.from(NOW),
        "ARBA01",
        of(FBM_WMS_INBOUND),
        of(CHECK_IN, PUT_AWAY),
        of(DATE_OUT),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW),
        Instant.from(NOW.plusHours(24))
    );

    // THEN
    assertEquals(consolidations.get(0).getDate(), result.get(0).getDate());
    assertEquals(consolidations.get(0).getTotal(), result.get(0).getTotal());
    assertEquals(consolidations.get(0).getKeys(), result.get(0).getKeys());
  }

  @Test
  void testExecuteProjectedBacklog() {
    // GIVEN
    final List<BacklogProjectionResponse> projectionsBacklog = getProjection();

    final BacklogProjectionInput backlogRequest = BacklogProjectionInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId("ARBA01")
        .processName(of(WAVING, PICKING, PACKING))
        .dateFrom(NOW)
        .dateTo(NOW)
        .groupType("order")
        .userId(1234L)
        .backlogs(null)
        .build();

    // WHEN
    when(backlogProjection.execute(backlogRequest)).thenReturn(projectionsBacklog);

    final List<BacklogProjectionResponse> result = backlogApiAdapter.getProjectedBacklog(
        "ARBA01",
        FBM_WMS_OUTBOUND,
        of(WAVING, PICKING, PACKING),
        NOW,
        NOW,
        1234L);

    // THEN
    assertEquals(projectionsBacklog.get(0).getProcessName(), result.get(0).getProcessName());
    assertEquals(projectionsBacklog.get(1).getProcessName(), result.get(1).getProcessName());
    assertEquals(projectionsBacklog.get(2).getProcessName(), result.get(2).getProcessName());
  }

  private List<Consolidation> getConsolidation() {

    final Map<String, String> mapDummy = new HashMap<>();

    return of(
        new Consolidation(Instant.from(NOW), mapDummy, 1, true),
        new Consolidation(Instant.from(NOW), mapDummy, 2, false));
  }

  private List<BacklogProjectionResponse> getProjection() {
    return of(new BacklogProjectionResponse(WAVING, of(new ProjectionValue(NOW, 1))),
              new BacklogProjectionResponse(PICKING, of(new ProjectionValue(NOW, 1))),
              new BacklogProjectionResponse(PACKING, of(new ProjectionValue(NOW, 1))));
  }
}
