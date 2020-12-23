package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.BacklogProjection;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
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
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBacklogProjectionTest {

    @InjectMocks
    private GetBacklogProjection getBacklogProjection;

    @Mock
    protected PlanningModelGateway planningModel;

    @Mock
    protected LogisticCenterGateway logisticCenter;

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
                        Map.of("status", "to_pack")
                ),
                WAREHOUSE_ID,
                A_DATE,
                A_DATE.plusHours(25))
        ).thenReturn(
                new ArrayList<>(
                        List.of(
                                ProcessBacklog.builder()
                                        .process("to_pack")
                                        .quantity(1442)
                                        .build()
                        ))
        );

        when(backlogGateway.getUnitBacklog("to_pick", WAREHOUSE_ID, A_DATE, A_DATE.plusHours(25)))
                .thenReturn(
                        ProcessBacklog.builder()
                                .process(ProcessInfo.PICKING.getStatus())
                                .quantity(2232)
                                .build()
            );

        final ZonedDateTime firstDate = getNextHour(A_DATE);
        when(planningModel.getBacklogProjection(BacklogProjectionRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(WORKFLOW)
                .processName(List.of(WAVING, PICKING, PACKING))
                .dateFrom(A_DATE)
                .dateTo(firstDate.plusHours(25))
                .currentBacklog(List.of(
                        new CurrentBacklog(WAVING, 0),
                        new CurrentBacklog(PICKING, 2232),
                        new CurrentBacklog(PACKING, 1442)))
                .build())).thenReturn(List.of(
                        new BacklogProjectionResponse(WAVING, List.of(
                                new ProjectionValue(firstDate, 100, "forecast"),
                                new ProjectionValue(firstDate.plusHours(1), 200, "forecast")
                        )),
                        new BacklogProjectionResponse(PICKING, List.of(
                                new ProjectionValue(firstDate, 120, "forecast"),
                                new ProjectionValue(firstDate.plusHours(1), 220, "forecast")
                        )),
                        new BacklogProjectionResponse(PACKING, List.of(
                                new ProjectionValue(firstDate, 130, "forecast"),
                                new ProjectionValue(firstDate.plusHours(1), 230, "forecast")
                        ))
        ));

        when(logisticCenter.getConfiguration(WAREHOUSE_ID)).thenReturn(
                new LogisticCenterConfiguration(getDefault()));

        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .workflow(WORKFLOW)
                .warehouseId(WAREHOUSE_ID)
                .processName(List.of(WAVING, PICKING, PACKING))
                .userId(1L)
                .dateFrom(A_DATE)
                .dateTo(A_DATE.plusHours(25))
                .build();

        // WHEN
        final BacklogProjection response = getBacklogProjection.execute(input);

        // THEN
        assertEquals("Proyecciones", response.getTitle());

        assertEquals(2, response.getTabs().size());
        assertEquals("cpt", response.getTabs().get(0).getType());
        assertEquals("backlog", response.getTabs().get(1).getType());

        assertEquals("Procesos", response.getSelections().getTitle());
        assertEquals(2, response.getSelections().getValues().size());
        assertEquals("picking", response.getSelections().getValues().get(0).getId());
        assertEquals("packing", response.getSelections().getValues().get(1).getId());

        final SimpleTable wavingSimpleTable = response.getSimpleTable1();
        assertEquals(26, wavingSimpleTable.getColumns().size());
        assertEquals(2, wavingSimpleTable.getData().size());

        final SimpleTable otherProcessesSimpleTable = response.getSimpleTable2();
        assertEquals(26, otherProcessesSimpleTable.getColumns().size());
        assertEquals(2, otherProcessesSimpleTable.getData().size());

        final Map<String, Object> rtwTargetData = wavingSimpleTable.getData().get(0);
        assertEquals("0", rtwTargetData.get("column_2"));
        assertEquals("0", rtwTargetData.get("column_3"));
        assertEquals("0", rtwTargetData.get("column_4"));

        final Map<String, Object> rtwRealData = wavingSimpleTable.getData().get(1);
        assertEquals("100", rtwRealData.get("column_2"));
        assertEquals("200", rtwRealData.get("column_3"));
        assertEquals("0", rtwRealData.get("column_4"));

        final Map<String, Object> rtpickRealData = otherProcessesSimpleTable.getData().get(0);
        assertEquals("120", rtpickRealData.get("column_2"));
        assertEquals("220", rtpickRealData.get("column_3"));
        assertEquals("0", rtpickRealData.get("column_4"));

        final Map<String, Object> rtpackRealData = otherProcessesSimpleTable.getData().get(1);
        assertEquals("130", rtpackRealData.get("column_2"));
        assertEquals("230", rtpackRealData.get("column_3"));
        assertEquals("0", rtpackRealData.get("column_4"));
    }
}
