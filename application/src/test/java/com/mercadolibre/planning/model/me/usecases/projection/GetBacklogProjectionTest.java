package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.BacklogProjection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.ORDER_GROUP_TYPE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetBacklogProjectionTest {

    @Mock
    protected PlanningModelGateway planningModel;
    @Mock
    protected LogisticCenterGateway logisticCenter;
    @InjectMocks
    private GetBacklogProjection getBacklogProjection;
    @Mock
    private ProjectBacklog projectBacklog;

    @Test
    public void testExecute() {
        // GIVEN
        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .workflow(WORKFLOW)
                .warehouseId(WAREHOUSE_ID)
                .processName(List.of(WAVING, PICKING, PACKING))
                .userId(1L)
                .dateFrom(A_DATE)
                .dateTo(A_DATE.plusHours(25))
                .groupType(ORDER_GROUP_TYPE)
                .build();

        final ZonedDateTime firstDate = getNextHour(A_DATE);

        when(planningModel.getEntities(EntityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .processName(List.of(PICKING))
                .entityType(REMAINING_PROCESSING)
                .dateFrom(A_DATE)
                .dateTo(A_DATE.plusHours(25))
                .build())
        ).thenReturn(List.of(
                Entity.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PICKING)
                        .date(firstDate)
                        .value(150)
                        .build(),
                Entity.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PICKING)
                        .date(firstDate.plusHours(1))
                        .value(210)
                        .build()
        ));

        when(projectBacklog.execute(input))
                .thenReturn(new ProjectedBacklog(List.of(
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
                        )))
                ));

        when(logisticCenter.getConfiguration(WAREHOUSE_ID)).thenReturn(
                new LogisticCenterConfiguration(getDefault()));

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
        assertEquals("150", rtwTargetData.get("column_2"));
        assertEquals("210", rtwTargetData.get("column_3"));
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
