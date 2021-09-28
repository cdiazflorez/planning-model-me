package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.ORDER_GROUP_TYPE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
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
    public void testExecute() {
        // GIVEN
        when(backlogGatewayProvider.getBy(WORKFLOW)).thenReturn(Optional.of(backlogGateway));

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
                WAREHOUSE_ID, A_DATE, A_DATE.plusHours(25), null, ORDER_GROUP_TYPE, true))
        ).thenReturn(
                ProcessBacklog.builder()
                        .process(ProcessInfo.PICKING.getStatus())
                        .quantity(2232)
                        .build()
        );

        final ZonedDateTime firstDate = getNextHour(A_DATE);

        when(planningModel.getBacklogProjection(
                BacklogProjectionRequest.builder()
                        .warehouseId(WAREHOUSE_ID)
                        .workflow(WORKFLOW)
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
                .workflow(WORKFLOW)
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

}
