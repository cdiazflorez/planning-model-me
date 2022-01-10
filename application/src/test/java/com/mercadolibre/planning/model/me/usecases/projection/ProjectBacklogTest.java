package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.ORDER_GROUP_TYPE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectBacklogTest {

    @Mock
    protected PlanningModelGateway planningModel;

    @InjectMocks
    private ProjectBacklog projectBacklog;

    @Mock
    private BacklogGatewayProvider backlogGatewayProvider;

    @Mock
    private BacklogGateway backlogGateway;

    @Test
    public void testExecuteOutbound() {
        // GIVEN
        when(backlogGatewayProvider.getBy(FBM_WMS_OUTBOUND)).thenReturn(Optional.of(backlogGateway));

        when(backlogGateway.getBacklog(
                List.of(
                        Map.of("status", "pending"),
                        Map.of("status", "to_pick"),
                        Map.of("status", "to_pack")
                ),
                WAREHOUSE_ID,
                A_DATE,
                A_DATE.plusHours(25),
                true)
        ).thenReturn(
                new ArrayList<>(
                        List.of(
                                ProcessBacklog.builder()
                                        .process("to_pack")
                                        .quantity(1442)
                                        .build()
                        ))
        );

        when(backlogGateway.getUnitBacklog(new UnitProcessBacklogInput("to_pick",
                WAREHOUSE_ID, A_DATE, A_DATE.plusHours(25), null, ORDER_GROUP_TYPE, false))
        ).thenReturn(
                ProcessBacklog.builder()
                        .process(ProcessOutbound.PICKING.getStatus())
                        .quantity(2232)
                        .build()
        );

        final ZonedDateTime firstDate = getNextHour(A_DATE);

        when(planningModel.getBacklogProjection(
                BacklogProjectionRequest.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(List.of(WAVING, PICKING, PACKING, PACKING_WALL))
                        .dateFrom(A_DATE)
                        .dateTo(firstDate.plusHours(25))
                        .currentBacklog(List.of(
                                new CurrentBacklog(WAVING, 0),
                                new CurrentBacklog(PICKING, 2232),
                                new CurrentBacklog(PACKING, 1442)))
                        .applyDeviation(true)
                        .build()))
                .thenReturn(List.of(
                        new BacklogProjectionResponse(WAVING, List.of(
                                new ProjectionValue(firstDate, 100),
                                new ProjectionValue(firstDate.plusHours(1), 200)
                        )),
                        new BacklogProjectionResponse(PICKING, List.of(
                                new ProjectionValue(firstDate, 120),
                                new ProjectionValue(firstDate.plusHours(1), 220)
                        )),
                        new BacklogProjectionResponse(PACKING, List.of(
                                new ProjectionValue(firstDate, 130),
                                new ProjectionValue(firstDate.plusHours(1), 230)
                        ))
                ));

        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .processName(List.of(WAVING, PICKING, PACKING))
                .userId(1L)
                .dateFrom(A_DATE)
                .dateTo(A_DATE.plusHours(25))
                .groupType(ORDER_GROUP_TYPE)
                .build();

        // WHEN
        final ProjectedBacklog response = projectBacklog.execute(input);

        // THEN
        assertNotNull(response);

        List<BacklogProjectionResponse> projections = response.getProjections();
        assertEquals(3, projections.size());

        BacklogProjectionResponse wavingProjections = projections.get(0);
        assertEquals(WAVING, wavingProjections.getProcessName());
        assertEquals(firstDate, wavingProjections.getValues().get(0).getDate());
        assertEquals(100, wavingProjections.getValues().get(0).getQuantity());
        assertEquals(200, wavingProjections.getValues().get(1).getQuantity());
    }

    @Test
    public void testExecuteInbound() {
        // GIVEN
        final ZonedDateTime firstDate = getNextHour(A_DATE);

        when(planningModel.getBacklogProjection(
                BacklogProjectionRequest.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(FBM_WMS_INBOUND)
                        .processName(List.of(CHECK_IN, PUT_AWAY))
                        .dateFrom(A_DATE)
                        .dateTo(firstDate.plusHours(25))
                        .currentBacklog(List.of(
                                new CurrentBacklog(CHECK_IN, 100),
                                new CurrentBacklog(PUT_AWAY, 120)))
                        .applyDeviation(true)
                        .build()))
                .thenReturn(List.of(
                        new BacklogProjectionResponse(CHECK_IN, List.of(
                                new ProjectionValue(firstDate, 100),
                                new ProjectionValue(firstDate.plusHours(1), 200)
                        )),
                        new BacklogProjectionResponse(PUT_AWAY, List.of(
                                new ProjectionValue(firstDate, 120),
                                new ProjectionValue(firstDate.plusHours(1), 220)
                        ))
                ));

        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .workflow(FBM_WMS_INBOUND)
                .warehouseId(WAREHOUSE_ID)
                .processName(List.of(CHECK_IN, PUT_AWAY))
                .userId(1L)
                .dateFrom(A_DATE)
                .dateTo(A_DATE.plusHours(25))
                .currentBacklog(List.of(
                        new Consolidation(Instant.from(firstDate), Map.of("process", "check_in"), 100),
                        new Consolidation(Instant.from(firstDate.plusHours(1)), Map.of("process", "check_in"), 200),
                        new Consolidation(Instant.from(firstDate), Map.of("process", "put_away"), 120),
                        new Consolidation(Instant.from(firstDate.plusHours(1)), Map.of("process", "put_away"), 220)))
                .build();

        // WHEN
        final ProjectedBacklog response = projectBacklog.execute(input);

        // THEN
        assertNotNull(response);

        List<BacklogProjectionResponse> projections = response.getProjections();
        assertEquals(2, projections.size());

        BacklogProjectionResponse checkinProjections = projections.get(0);
        assertEquals(CHECK_IN, checkinProjections.getProcessName());
        assertEquals(firstDate, checkinProjections.getValues().get(0).getDate());
        assertEquals(100, checkinProjections.getValues().get(0).getQuantity());
        assertEquals(200, checkinProjections.getValues().get(1).getQuantity());
    }
}
