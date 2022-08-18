package com.mercadolibre.planning.model.me.usecases.projection.deferral;

import static com.mercadolibre.planning.model.me.enums.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.TimeZone.getDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.clock.RequestClockGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.projection.deferral.DeferralProjectionStatus;
import com.mercadolibre.planning.model.me.usecases.projection.ProjectionDataMapper;
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
  private static final ZonedDateTime GET_CURRENT_UTC_DATE_TIME = now().withZoneSameInstant(UTC);

  private static final ZonedDateTime CPT_0 = GET_CURRENT_UTC_DATE_TIME.truncatedTo(ChronoUnit.HOURS); //current hour

  private static final ZonedDateTime CPT_1 = CPT_0.plusHours(4);

  private static final ZonedDateTime CPT_2 = CPT_0.plusHours(5);

  private static final ZonedDateTime CPT_3 = CPT_0.plusHours(6);

  @InjectMocks
  private GetDeferralProjection getDeferralProjection;

  @Mock
  private ProjectionDataMapper getProjectionDataMapper;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Mock
  private GetSimpleDeferralProjection getSimpleDeferralProjection;

  @Mock
  private RequestClockGateway requestClockGateway;

  @Mock
  private ProjectionGateway projectionGateway;

  @Test
  public void testExecute() {
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
            mockProjectionResult(),
            new LogisticCenterConfiguration(getDefault())));

    // when the input date are different from the current hour (at least hour of difference)
    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        currentUtcDate.plusHours(1),
        mockBacklog(),
        false,
        null)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionResult(),
            new LogisticCenterConfiguration(getDefault())));

    // when the input date are null
    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        null,
        mockBacklog(),
        false,
        emptyList())))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionResult(),
            new LogisticCenterConfiguration(getDefault())));

    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        null,
        mockBacklog(),
        false,
        mockSimulations())))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionResultWithSimulations(),
            new LogisticCenterConfiguration(getDefault())));

    when(projectionGateway.getDeferralProjectionStatus(
        any(Instant.class),
        any(Instant.class),
        any(WORKFLOW.getClass()),
        any(),
        any(),
        any(String.class),
        any(String.class),
        any(boolean.class),
        any()))
        .thenReturn(List.of(new DeferralProjectionStatus(CPT_0.toInstant(),
            CPT_0.toInstant().minusSeconds(60L),
            30,
            "cap_max")));

    // WHEN
    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        currentUtcDate.plusHours(1),
        mockBacklog(),
        false,
        null)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionResult(),
            new LogisticCenterConfiguration(getDefault())));

    final PlanningView projection = getDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        currentUtcDate,
        mockBacklog(),
        false,
        null));

    final PlanningView projectionFutureInputDate = getDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        currentUtcDate.plusHours(1),
        mockBacklog(),
        false,
        null));

    final PlanningView projectionNullInputDate = getDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        null,
        mockBacklog(),
        false,
        mockSimulations()));

    //THEN
    assertEquals(3, projection.getData().getProjections().size());
    assertFalse(projection.getData().getProjections().get(0).isDeferred());
    assertFalse(projection.getData().getProjections().get(1).isDeferred());
    assertFalse(projection.getData().getProjections().get(2).isDeferred());
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
    assertEquals(expectedCPTs, projectionFutureInputDate.getData().getProjections().size());
    assertEquals(3, projectionFutureInputDate.getData().getProjections().size());
    assertEquals(3, projectionNullInputDate.getData().getProjections().size());
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
    verifyNoInteractions(getProjectionDataMapper);
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
    verifyNoInteractions(getProjectionDataMapper);
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
            mockProjectionResult(),
            new LogisticCenterConfiguration(getDefault())));

    // WHEN
    final PlanningView projection = getDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        currentUtcDate,
        mockBacklog(),
        false,
        null));

    //THEN
    assertNull(projection.getData());
  }

  private List<Backlog> mockBacklog() {
    return List.of(
        new Backlog(CPT_1, 150),
        new Backlog(CPT_2, 235),
        new Backlog(CPT_3, 300)
    );
  }

  private List<Simulation> mockSimulations() {
    return List.of(new Simulation(
        GLOBAL,
        List.of(
            new SimulationEntity(MagnitudeType.MAX_CAPACITY,
                List.of(new QuantityByDate(CPT_1, 300))
            )
        )
    ));

  }

  private List<ProjectionResult> mockProjectionResult() {
    return List.of(
        ProjectionResult.builder()
            .date(CPT_0)
            .projectedEndDate(CPT_1.plusMinutes(-300))
            .remainingQuantity(0)
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

  private List<ProjectionResult> mockProjectionResultWithSimulations() {
    return List.of(
        ProjectionResult.builder()
            .date(CPT_0)
            .projectedEndDate(CPT_1.plusMinutes(-200))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_1)
            .projectedEndDate(CPT_1.plusMinutes(-200))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_2)
            .projectedEndDate(CPT_2.plusMinutes(100))
            .remainingQuantity(200)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_3)
            .projectedEndDate(CPT_3.plusMinutes(-200))
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
                .source(Source.FORECAST)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1.plusHours(-3))
                .processName(GLOBAL)
                .value(20)
                .source(Source.FORECAST)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1.plusHours(-2))
                .processName(GLOBAL)
                .value(15)
                .source(Source.FORECAST)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1.plusHours(-1))
                .processName(GLOBAL)
                .value(30)
                .source(Source.FORECAST)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1)
                .processName(GLOBAL)
                .value(79)
                .source(Source.FORECAST)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1.plusDays(1))
                .processName(GLOBAL)
                .value(32)
                .source(Source.FORECAST)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1.plusDays(1))
                .processName(GLOBAL)
                .value(35)
                .source(Source.SIMULATION)
                .build(),
            MagnitudePhoto.builder()
                .date(CPT_1.plusHours(-4))
                .processName(GLOBAL)
                .value(10)
                .source(Source.SIMULATION)
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
}
