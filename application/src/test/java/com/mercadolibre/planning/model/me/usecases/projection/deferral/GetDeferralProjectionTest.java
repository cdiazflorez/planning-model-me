package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDeferralProjectionTest {

    private static final ZonedDateTime CPT_0 = getCurrentUtcDate().plusHours(2);
    private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(6);
    private static final String MX_LOGISTIC_CENTER = "MXCD01";

    @InjectMocks
    private GetDeferralProjection getDeferralProjection;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private GetProjectionSummary getProjectionSummary;

    @Mock
    private BacklogGateway backlogGateway;

    @Mock
    private GetSimpleDeferralProjection getSimpleDeferralProjection;

    @Test
    public void testExecute() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(planningModelGateway.getEntities(any(EntityRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        when(backlogGateway.getBacklog(
                WAREHOUSE_ID,
                currentUtcDateTime,
                currentUtcDateTime.plusDays(3),
                List.of("pending"),
                List.of("etd")))
                .thenReturn(mockBacklog());

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false,
                false)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false,
                false));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(2, projection.getTabs().size());
        assertEquals(false, projection.getData().getChart().getData().get(0).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(1).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(2).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(3).getIsDeferred());
        assertEquals("throughput", projection.getData().getComplexTable1().getData()
                .get(0).getId());
        assertEquals("Throughput", projection.getData().getComplexTable1().getData()
                .get(0).getTitle());
    }

    @Test
    public void testExecute20Cap5Logic() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(planningModelGateway.getEntities(any(EntityRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        when(backlogGateway.getBacklog(
                MX_LOGISTIC_CENTER,
                currentUtcDateTime,
                currentUtcDateTime.plusDays(3),
                List.of("pending", "planning", "to_pick", "picking", "sorting",
                        "to_group", "grouping", "grouped", "to_pack"),
                List.of("etd", "status")))
                .thenReturn(mockBacklogAndBacklogInProcess());

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                MX_LOGISTIC_CENTER, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                List.of(
                        new Backlog(CPT_1, "pending",750),
                        new Backlog(CPT_2, "pending",235),
                        new Backlog(CPT_3, "pending",300)),
                true,
                false)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                MX_LOGISTIC_CENTER,
                FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                null,
                true,
                false));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(2, projection.getTabs().size());
        assertEquals(false, projection.getData().getChart().getData().get(0).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(1).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(2).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(3).getIsDeferred());
        assertEquals("throughput", projection.getData().getComplexTable1().getData()
                .get(0).getId());
        assertEquals("Throughput", projection.getData().getComplexTable1().getData()
                .get(0).getTitle());
    }

    @Test
    public void testExecute21Cap5Logic() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(planningModelGateway.getEntities(any(EntityRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        when(backlogGateway.getBacklog(
                MX_LOGISTIC_CENTER,
                currentUtcDateTime,
                currentUtcDateTime.plusDays(3),
                List.of("pending", "planning", "to_pick", "picking", "sorting",
                        "to_group", "grouping", "grouped", "to_pack"),
                List.of("etd")))
                .thenReturn(mockBacklog());

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                MX_LOGISTIC_CENTER, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false,
                true)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                MX_LOGISTIC_CENTER,
                FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                null,
                false,
                true));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(2, projection.getTabs().size());
        assertEquals(false, projection.getData().getChart().getData().get(0).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(1).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(2).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(3).getIsDeferred());
        assertEquals("throughput", projection.getData().getComplexTable1().getData()
                .get(0).getId());
        assertEquals("Throughput", projection.getData().getComplexTable1().getData()
                .get(0).getTitle());
    }

    @Test
    public void testExecuteWithError() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false,
                false)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false,
                false));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(2, projection.getTabs().size());
        assertNull(projection.getData());
    }

    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(CPT_1, 150),
                new Backlog(CPT_2, 235),
                new Backlog(CPT_3, 300)
        );
    }

    private List<Backlog> mockBacklogAndBacklogInProcess() {
        final List<Backlog> backlogs = new ArrayList<>();
        backlogs.add(new Backlog(CPT_1, "pending", 150));
        backlogs.add(new Backlog(CPT_2, "pending", 235));
        backlogs.add(new Backlog(CPT_3, "pending", 300));
        backlogs.add(new Backlog(CPT_2, "to_pick", 200));
        backlogs.add(new Backlog(CPT_2, "sorting", 200));
        backlogs.add(new Backlog(CPT_2, "to_pack", 200));
        return backlogs;
    }

    private List<ProjectionResult> mockProjections() {
        return List.of(
                ProjectionResult.builder()
                        .date(CPT_0)
                        .remainingQuantity(1000)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_1)
                        .projectedEndDate(CPT_1.plusMinutes(-300))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_2)
                        .projectedEndDate(CPT_2.plusMinutes(120))
                        .remainingQuantity(200)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_3)
                        .projectedEndDate(CPT_3.plusMinutes(-240))
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .isDeferred(false)
                        .build()
        );
    }

    private List<Entity> mockHeadcountEntities() {
        return List.of(
                Entity.builder()
                        .date(CPT_1.plusHours(-4))
                        .processName(GLOBAL)
                        .value(10)
                        .build(),
                Entity.builder()
                        .date(CPT_1.plusHours(-3))
                        .processName(GLOBAL)
                        .value(20)
                        .build(),
                Entity.builder()
                        .date(CPT_1.plusHours(-2))
                        .processName(GLOBAL)
                        .value(15)
                        .build(),
                Entity.builder()
                        .date(CPT_1.plusHours(-1))
                        .processName(GLOBAL)
                        .value(30)
                        .build(),
                Entity.builder()
                        .date(CPT_1)
                        .processName(GLOBAL)
                        .value(79)
                        .build(),
                Entity.builder()
                        .date(CPT_1.plusDays(1))
                        .processName(GLOBAL)
                        .value(32)
                        .build()
        );
    }

    private SimpleTable mockSimpleTable() {
        return new SimpleTable(
                "title",
                emptyList(),
                emptyList()
        );
    }
}
