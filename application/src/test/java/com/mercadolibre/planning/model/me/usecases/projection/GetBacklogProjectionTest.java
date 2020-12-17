package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.BacklogProjection;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.strategy.BacklogGatewayProvider;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklog;
import com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.utils.DateUtils;
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
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata.process.ProcessInfo.WALL_IN;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBacklogProjectionTest {

    private static final ZonedDateTime DATE_1 = ZonedDateTime.now();
    private static final ZonedDateTime DATE_2 = ZonedDateTime.now().plusHours(1);

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
        final ZonedDateTime dateFrom = DateUtils.getCurrentUtcDate();
        final BacklogProjectionInput input = new BacklogProjectionInput(
                Workflow.FBM_WMS_OUTBOUND,
                "ARBA01",
                List.of(WAVING, PICKING, PACKING),
                1L);
        when(logisticCenter.getConfiguration(input.getWarehouseId())).thenReturn(
                new LogisticCenterConfiguration(getDefault()));

        final String status = "status";
        final List<Map<String, String>> statuses = List.of(
                Map.of(status, OUTBOUND_PLANNING.getStatus()),
                Map.of(status, ProcessInfo.PACKING.getStatus())
        );
        when(backlogGatewayProvider.getBy(input.getWorkflow()))
                .thenReturn(Optional.of(backlogGateway));

        when(backlogGateway.getBacklog(statuses,
                input.getWarehouseId(),
                dateFrom,
                dateFrom.plusHours(25)
        )).thenReturn(
                new ArrayList<>(
                        List.of(
                                com.mercadolibre.planning.model.me.entities.projection
                                        .ProcessBacklog.builder()
                                        .process(ProcessInfo.PACKING.getStatus())
                                        .quantity(1442)
                                        .build()
                        ))
        );
        when(backlogGateway.getUnitBacklog(ProcessInfo.PICKING.getStatus(),
                input.getWarehouseId(),
                dateFrom,
                dateFrom.plusHours(25)
        )).thenReturn(
                com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog.builder()
                        .process(ProcessInfo.PICKING.getStatus())
                        .quantity(2232)
                        .build()
        );

        when(planningModel.getBacklogProjection(BacklogProjectionRequest.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processName(input.getProcessName())
                .dateFrom(dateFrom)
                .dateTo(dateFrom.plusHours(25L))
                .currentBacklog(List.of(
                        new ProcessBacklog(WAVING, 0),
                        new ProcessBacklog(PICKING, 2232),
                        new ProcessBacklog(PACKING, 1442)))
                .build())).thenReturn(List.of(
                        new BacklogProjectionResponse(WAVING, List.of(
                                new ProjectionValue(DATE_1, 100, "forecast"),
                                new ProjectionValue(DATE_2, 200, "forecast"),
                                new ProjectionValue(DATE_1, 150, null),
                                new ProjectionValue(DATE_2, 250, null)
                        )),
                        new BacklogProjectionResponse(PICKING, List.of(
                                new ProjectionValue(DATE_1, 120, null),
                                new ProjectionValue(DATE_2, 220, null)
                        )),
                        new BacklogProjectionResponse(PACKING, List.of(
                                new ProjectionValue(DATE_1, 130, null),
                                new ProjectionValue(DATE_2, 230, null)
                        ))
        ));

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

        assertEquals(26, response.getSimpleTable1().getColumns().size());
        assertEquals(2, response.getSimpleTable1().getData().size());
        assertEquals("100", response.getSimpleTable1().getData().get(0).get("column_2"));
        assertEquals("200", response.getSimpleTable1().getData().get(0).get("column_3"));
        assertEquals("0", response.getSimpleTable1().getData().get(0).get("column_4"));
        assertEquals("150", response.getSimpleTable1().getData().get(1).get("column_2"));
        assertEquals("250", response.getSimpleTable1().getData().get(1).get("column_3"));
        assertEquals("0", response.getSimpleTable1().getData().get(1).get("column_4"));

        assertEquals(26, response.getSimpleTable2().getColumns().size());
        assertEquals(2, response.getSimpleTable2().getData().size());
        assertEquals("120", response.getSimpleTable2().getData().get(0).get("column_2"));
        assertEquals("220", response.getSimpleTable2().getData().get(0).get("column_3"));
        assertEquals("0", response.getSimpleTable2().getData().get(0).get("column_4"));
        assertEquals("130", response.getSimpleTable2().getData().get(1).get("column_2"));
        assertEquals("230", response.getSimpleTable2().getData().get(1).get("column_3"));
        assertEquals("0", response.getSimpleTable2().getData().get(1).get("column_4"));

    }
}
