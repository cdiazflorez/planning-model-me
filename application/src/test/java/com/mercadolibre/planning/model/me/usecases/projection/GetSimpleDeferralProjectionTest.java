package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSimpleDeferralProjectionTest {

    private static final ZonedDateTime CPT_DATE_1 = getCurrentUtcDate();
    private static final ZonedDateTime CPT_DATE_2 = getCurrentUtcDate().plusHours(1);
    private static final ZonedDateTime CPT_DATE_3 = getCurrentUtcDate().plusHours(2);
    private static final ZonedDateTime CPT_DATE_4 = getCurrentUtcDate().plusHours(3);
    private static final ZonedDateTime CPT_DATE_5 = getCurrentUtcDate().plusHours(4);
    private static final ZonedDateTime PROJECT_DATE = getCurrentUtcDate();

    @InjectMocks
    private GetSimpleDeferralProjection getSimpleDeferralProjection;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Test
    public void textExecute() {

        // GIVEN
        final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();

        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(
                new LogisticCenterConfiguration(getDefault()));

        when(planningModelGateway.runDeferralProjection(any(ProjectionRequest.class)))
                .thenReturn(mockProjections());

        // WHEN
        final GetSimpleDeferralProjectionOutput results = getSimpleDeferralProjection.execute(
                new GetProjectionInput(
                        WAREHOUSE_ID,
                        FBM_WMS_OUTBOUND,
                        currentUtcDateTime,
                        mockBacklog(),
                        false));

        // THEN
        assertEquals(false, results.getProjections().get(0).isDeferred());
        assertEquals(false, results.getProjections().get(1).isDeferred());
        assertEquals(true, results.getProjections().get(2).isDeferred());
        assertEquals(true, results.getProjections().get(3).isDeferred());
        assertEquals(false, results.getProjections().get(4).isDeferred());
    }

    private List<ProjectionResult> mockProjections() {
        return List.of(
                ProjectionResult.builder()
                        .date(CPT_DATE_1)
                        .projectedEndDate(PROJECT_DATE)
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(30, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_DATE_2)
                        .projectedEndDate(PROJECT_DATE)
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(30, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_DATE_3)
                        .projectedEndDate(PROJECT_DATE)
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(30, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_DATE_4)
                        .projectedEndDate(PROJECT_DATE)
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(240, MINUTES.getName()))
                        .isDeferred(false)
                        .build(),
                ProjectionResult.builder()
                        .date(CPT_DATE_5)
                        .projectedEndDate(PROJECT_DATE)
                        .remainingQuantity(0)
                        .processingTime(new ProcessingTime(30, MINUTES.getName()))
                        .isDeferred(false)
                        .build()
        );
    }

    private List<Backlog> mockBacklog() {
        return List.of(
                new Backlog(CPT_DATE_1, 150),
                new Backlog(CPT_DATE_2, 235),
                new Backlog(CPT_DATE_3, 300)
        );
    }
}
