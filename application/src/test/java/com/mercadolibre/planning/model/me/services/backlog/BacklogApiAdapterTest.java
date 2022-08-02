package com.mercadolibre.planning.model.me.services.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectBacklog;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
                List.of("outbound-orders"),
                List.of(WAVING.getName(), PICKING.getName(), PACKING.getName()),
                emptyList(),
                List.of("date_out"),
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
                List.of(FBM_WMS_OUTBOUND),
                List.of(WAVING, PICKING, PACKING),
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
        final ProjectedBacklog projectedBacklog = getProjection();

        final BacklogProjectionInput backlogRequest = BacklogProjectionInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId("ARBA01")
                .processName(List.of(WAVING, PICKING, PACKING))
                .dateFrom(NOW)
                .dateTo(NOW)
                .groupType("order")
                .userId(1234L)
                .backlogs(null)
                .build();

        // WHEN
        when(backlogProjection.execute(backlogRequest)).thenReturn(projectedBacklog);

        final List<BacklogProjectionResponse> result = backlogApiAdapter.getProjectedBacklog(
                "ARBA01",
                FBM_WMS_OUTBOUND,
                List.of(WAVING, PICKING, PACKING),
                NOW,
                NOW,
                1234L,
                emptyList());

        // THEN
        assertEquals(projectedBacklog.getProjections().get(0).getProcessName(), result.get(0).getProcessName());
        assertEquals(projectedBacklog.getProjections().get(1).getProcessName(), result.get(1).getProcessName());
        assertEquals(projectedBacklog.getProjections().get(2).getProcessName(), result.get(2).getProcessName());
    }

    private List<Consolidation> getConsolidation() {

        final Map<String, String> mapDummy = new HashMap<>();

        return List.of(
                new Consolidation(Instant.from(NOW), mapDummy, 1, true),
                new Consolidation(Instant.from(NOW), mapDummy, 2, false));
    }

    private ProjectedBacklog getProjection() {

        return new ProjectedBacklog(
                List.of(new BacklogProjectionResponse(WAVING, List.of(new ProjectionValue(NOW, 1))),
                        new BacklogProjectionResponse(PICKING, List.of(new ProjectionValue(NOW, 1))),
                        new BacklogProjectionResponse(PACKING, List.of(new ProjectionValue(NOW, 1)))));
    }
}
