package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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

    @InjectMocks
    private GetDeferralProjection getDeferralProjection;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private GetProjectionSummary getProjectionSummary;

    @Mock
    private BacklogApiGateway backlogGateway;

    @Mock
    private GetSimpleDeferralProjection getSimpleDeferralProjection;

    @Test
    public void testExecute() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(planningModelGateway.getTrajectories(any(TrajectoriesRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        when(backlogGateway.getCurrentBacklog(
                WAREHOUSE_ID,
                List.of("outbound-orders"),
                List.of("pending", "planning", "to_pick", "picking", "sorting",
                        "to_group", "grouping", "grouped", "to_pack"),
                currentUtcDateTime.toInstant(),
                currentUtcDateTime.plusDays(3).toInstant(),
                List.of("date_out"))
        ).thenReturn(List.of(
                new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150),
                new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235),
                new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300)
        ));

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(1, projection.getTabs().size());
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
                false)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDateTime,
                mockBacklog(),
                false));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(1, projection.getTabs().size());
        assertNull(projection.getData());
    }

    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(CPT_1, 150),
                new Backlog(CPT_2, 235),
                new Backlog(CPT_3, 300)
        );
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

    private List<MagnitudePhoto> mockHeadcountEntities() {
        return List.of(
                MagnitudePhoto.builder()
                        .date(CPT_1.plusHours(-4))
                        .processName(GLOBAL)
                        .value(10)
                        .build(),
                MagnitudePhoto.builder()
                        .date(CPT_1.plusHours(-3))
                        .processName(GLOBAL)
                        .value(20)
                        .build(),
                MagnitudePhoto.builder()
                        .date(CPT_1.plusHours(-2))
                        .processName(GLOBAL)
                        .value(15)
                        .build(),
                MagnitudePhoto.builder()
                        .date(CPT_1.plusHours(-1))
                        .processName(GLOBAL)
                        .value(30)
                        .build(),
                MagnitudePhoto.builder()
                        .date(CPT_1)
                        .processName(GLOBAL)
                        .value(79)
                        .build(),
                MagnitudePhoto.builder()
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
