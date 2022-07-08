package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.clock.RequestClockGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetDeferralProjectionTest {

    private static final ZonedDateTime GET_CURRENT_UTC_DATE_TIME = getCurrentUtcDateTime();

    private static final ZonedDateTime CPT_0 = GET_CURRENT_UTC_DATE_TIME.truncatedTo(ChronoUnit.HOURS); //current hour

    private static final ZonedDateTime CPT_1 = CPT_0.plusHours(4);

    private static final ZonedDateTime CPT_2 = CPT_0.plusHours(5);

    private static final ZonedDateTime CPT_3 = CPT_0.plusHours(6);

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

    @Mock
    private RequestClockGateway requestClockGateway;

    @Test
    public void testExecute() {
        // GIVEN
        final ZonedDateTime currentUtcDate = CPT_0;

        when(requestClockGateway.now()).thenReturn(GET_CURRENT_UTC_DATE_TIME.toInstant());

        when(planningModelGateway.searchTrajectories(any(SearchTrajectoriesRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
                .thenReturn(mockSimpleTable());

        when(backlogGateway.getCurrentBacklog(
                WAREHOUSE_ID,
                List.of("outbound-orders"),
                List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted",
                        "to_group", "grouping", "grouped", "to_pack"),
                currentUtcDate.toInstant(),
                currentUtcDate.plusDays(3).toInstant(),
                List.of("date_out"))
        ).thenReturn(List.of(
                new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
                new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
                new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true)
        ));

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // when the input date are different from the current hour (at least hour of difference)
        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate.plusHours(1),
                mockBacklog(),
                false,
                null)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // when the input date are null
        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                null,
                mockBacklog(),
                false,
                null)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null));

        final Projection projectionFutureInputDate = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate.plusHours(1),
                mockBacklog(),
                false,
                null));

        final Projection projectionNullInputDate = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                null,
                mockBacklog(),
                false,
                null));

        //THEN
        assertEquals("Proyección", projection.getTitle());
        assertEquals(1, projection.getTabs().size());
        assertEquals(false, projection.getData().getChart().getData().get(0).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(1).getIsDeferred());
        assertEquals(false, projection.getData().getChart().getData().get(2).getIsDeferred());
        assertEquals("max_capacity", projection.getData().getComplexTable1().getData()
                .get(0).getId());
        assertEquals("Throughput", projection.getData().getComplexTable1().getData()
                .get(0).getTitle());
        assertTrue(projection.getData().getComplexTable1().getData()
                .get(0).getContent().get(0).get("column_2").isValid());

        //check if the first CPT and the current date are in the same hour and minute, if is the case then the CPT_0 have to be in the projection, otherwise
        //the CPT_0 shouldn't be because there is not in the date range anymore
        final Instant selectedDate = currentUtcDate.truncatedTo(ChronoUnit.MINUTES).toInstant();
        final Instant currentDate = GET_CURRENT_UTC_DATE_TIME.toInstant();
        final boolean cptAreSameHourMinuteWithCurrentDate = selectedDate.equals(currentDate);

        final int expectedCPTs = cptAreSameHourMinuteWithCurrentDate ? 4 : 3;
        assertEquals(expectedCPTs, projection.getData().getChart().getData().size());
        assertEquals(3, projectionFutureInputDate.getData().getChart().getData().size());
        assertEquals(3, projectionNullInputDate.getData().getChart().getData().size());
    }

    @Test
    public void testExecuteNoProjections() {
        // GIVEN
        final ZonedDateTime currentUtcDate = CPT_0;

        when(requestClockGateway.now()).thenReturn(GET_CURRENT_UTC_DATE_TIME.toInstant());

        when(planningModelGateway.searchTrajectories(any(SearchTrajectoriesRequest.class))).thenReturn(
                mockHeadcountEntities());

        when(backlogGateway.getCurrentBacklog(
                WAREHOUSE_ID,
                List.of("outbound-orders"),
                List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted",
                        "to_group", "grouping", "grouped", "to_pack"),
                currentUtcDate.toInstant(),
                currentUtcDate.plusDays(3).toInstant(),
                List.of("date_out"))
        ).thenReturn(List.of(
                new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
                new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
                new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true)
        ));

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        emptyList(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null));

        //THEN
        verifyNoInteractions(getProjectionSummary);
    }

    @Test
    public void testExecuteNullProjections() {
        // GIVEN
        final ZonedDateTime currentUtcDate = CPT_0;

        when(requestClockGateway.now()).thenReturn(GET_CURRENT_UTC_DATE_TIME.toInstant());

        when(backlogGateway.getCurrentBacklog(
                WAREHOUSE_ID,
                List.of("outbound-orders"),
                List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted",
                        "to_group", "grouping", "grouped", "to_pack"),
                currentUtcDate.toInstant(),
                currentUtcDate.plusDays(3).toInstant(),
                List.of("date_out"))
        ).thenReturn(List.of(
                new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
                new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
                new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true)
        ));

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        null,
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null));

        //THEN
        verifyNoInteractions(getProjectionSummary);
    }

    @Test
    public void testExecuteWithError() {
        // GIVEN
        final ZonedDateTime currentUtcDate = CPT_0;

        when(requestClockGateway.now()).thenReturn(GET_CURRENT_UTC_DATE_TIME.toInstant());

        when(getSimpleDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null)))
                .thenReturn(new GetSimpleDeferralProjectionOutput(
                        mockProjections(),
                        new LogisticCenterConfiguration(getDefault())));

        // WHEN
        final Projection projection = getDeferralProjection.execute(new GetProjectionInput(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND,
                currentUtcDate,
                mockBacklog(),
                false,
                null));

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

    private Map<MagnitudeType, List<MagnitudePhoto>> mockHeadcountEntities() {
        return Map.of(
                MagnitudeType.HEADCOUNT, List.of(
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
                ),
                MagnitudeType.THROUGHPUT, List.of(
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-4))
                                .processName(PACKING)
                                .value(5)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-3))
                                .processName(PACKING)
                                .value(10)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-2))
                                .processName(PACKING)
                                .value(7)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-1))
                                .processName(PACKING)
                                .value(15)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1)
                                .processName(PACKING)
                                .value(39)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusDays(1))
                                .processName(PACKING)
                                .value(16)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-4))
                                .processName(PACKING_WALL)
                                .value(5)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-3))
                                .processName(PACKING_WALL)
                                .value(10)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-2))
                                .processName(PACKING_WALL)
                                .value(10)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusHours(-1))
                                .processName(PACKING_WALL)
                                .value(15)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1)
                                .processName(PACKING_WALL)
                                .value(45)
                                .build(),
                        MagnitudePhoto.builder()
                                .date(CPT_1.plusDays(1))
                                .processName(PACKING_WALL)
                                .value(16)
                                .build()
                ));
    }

    private SimpleTable mockSimpleTable() {
        return new SimpleTable(
                "title",
                emptyList(),
                emptyList()
        );
    }
}
