package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDate;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDeferralProjectionTest {

    private static final ZonedDateTime CPT_0 = getCurrentUtcDateTime().plusHours(2);
    private static final ZonedDateTime CPT_1 = getCurrentUtcDateTime().plusHours(4);
    private static final ZonedDateTime CPT_2 = getCurrentUtcDateTime().plusHours(5);
    private static final ZonedDateTime CPT_3 = getCurrentUtcDateTime().plusHours(6);

    @InjectMocks
    private GetDeferralProjection getDeferralProjection;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private GetBacklogByDate getBacklog;

    @Mock
    private GetProjectionSummary getProjectionSummary;

    @Test
    public void testExecute() {
        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(
                new LogisticCenterConfiguration(getDefault()));

        when(getBacklog.execute(new GetBacklogByDateDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID,
                currentUtcDateTime, currentUtcDateTime.plusDays(3))))
                .thenReturn(mockBacklog());

        when(planningModelGateway.runDeferralProjection(any(ProjectionRequest.class)))
                .thenReturn(mockProjections());

        when(planningModelGateway.getEntities(any(EntityRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND, null));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(2, projection.getTabs().size());
        assertEquals(false, projection.getData().getChart().getData().get(0).getIsDeferred());
        assertEquals(true, projection.getData().getChart().getData().get(1).getIsDeferred());
        assertEquals(true, projection.getData().getChart().getData().get(2).getIsDeferred());
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

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(
                new LogisticCenterConfiguration(getDefault()));
        when(getBacklog.execute(new GetBacklogByDateDto(FBM_WMS_OUTBOUND, WAREHOUSE_ID,
                currentUtcDateTime, currentUtcDateTime.plusDays(3))))
                .thenReturn(mockBacklog());
        when(planningModelGateway.runDeferralProjection(any(ProjectionRequest.class)))
                .thenThrow(RuntimeException.class);

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND, null));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(2, projection.getTabs().size());
        assertEquals(null, projection.getData());
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
