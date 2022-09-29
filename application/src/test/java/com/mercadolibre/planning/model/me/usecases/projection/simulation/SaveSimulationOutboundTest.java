package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockPlanningBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.ProjectionWorkflow.getSteps;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.CycleTimeRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SlaProperties;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.PackingRatio;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link SaveSimulationOutbound}.
 */
@ExtendWith(MockitoExtension.class)
class SaveSimulationOutboundTest {

  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

  private static final Instant OPERATING_HOUR_0 = getCurrentUtcDate().toInstant();

  private static final Instant OPERATING_HOUR_1 = OPERATING_HOUR_0.plus(60L, ChronoUnit.MINUTES);

  private static final Instant OPERATING_HOUR_2 = OPERATING_HOUR_0.plus(120L, ChronoUnit.MINUTES);

  private static final Instant OPERATING_HOUR_3 = OPERATING_HOUR_0.plus(180L, ChronoUnit.MINUTES);

  private static final List<MagnitudePhoto> THROUGHPUT_AT_PROCESS = Stream.of(
          throughputs(PICKING, 100, 100, 100, 100),
          throughputs(PACKING, 200, 200, 200, 200),
          throughputs(BATCH_SORTER, 150, 200, 250, 300),
          throughputs(WALL_IN, 200, 250, 350, 300),
          throughputs(PACKING_WALL, 300, 350, 400, 400)
      )
      .flatMap(Function.identity())
      .collect(toList());

  private static final Map<ProcessName, Map<Instant, Integer>> THROUGHPUT_PER_PROCESS = Map.of(
      PICKING, throughputs(100, 100, 100, 100),
      PACKING, throughputs(200, 200, 200, 200),
      BATCH_SORTER, throughputs(150, 200, 250, 300),
      WALL_IN, throughputs(200, 250, 350, 300),
      PACKING_WALL, throughputs(300, 350, 400, 400)
  );

  private static final PackingRatio DEFAULT_RATIO = new PackingRatio(0.5, 0.5);

  private static final Map<Instant, PackingRatio> RATIOS = Map.of(
      OPERATING_HOUR_0, DEFAULT_RATIO,
      OPERATING_HOUR_1, DEFAULT_RATIO,
      OPERATING_HOUR_2, DEFAULT_RATIO,
      OPERATING_HOUR_3, DEFAULT_RATIO
  );

  private static final String NO_AREA = "";

  private static final Instant CPT_0 = OPERATING_HOUR_0.plus(360L, ChronoUnit.MINUTES);

  private static final Instant CPT_0_RESULT = CPT_0.minus(45L, ChronoUnit.MINUTES);

  private static final Instant CPT_1 = CPT_0.plus(60L, ChronoUnit.MINUTES);

  private static final Instant CPT_1_RESULT = CPT_1.minus(60L, ChronoUnit.MINUTES);

  private static final Instant CPT_2 = CPT_0.plus(120L, ChronoUnit.MINUTES);

  private static final Instant CPT_2_RESULT = CPT_2.plus(20L, ChronoUnit.MINUTES);

  private static final Instant CPT_3 = CPT_0.plus(180L, ChronoUnit.MINUTES);

  private static final Instant CPT_3_RESULT = getCurrentTime().toInstant();

  private static final List<Photo.Group> PHOTO_GROUPS = List.of(
      photoGroup(Step.PENDING, CPT_0, NO_AREA, 1200),
      photoGroup(Step.TO_PICK, CPT_1, NO_AREA, 980),
      photoGroup(Step.TO_PACK, CPT_2, NO_AREA, 600),
      photoGroup(Step.TO_PACK, CPT_2, "PW", 120)
  );

  private static final Map<Instant, SlaProperties> CYCLE_TIMES = Map.of(
      CPT_0, new SlaProperties(40L),
      CPT_1, new SlaProperties(50L),
      CPT_2, new SlaProperties(50L),
      CPT_3, new SlaProperties(60L)
  );

  private static final Map<Instant, ProcessingTime> PROCESSING_TIMES = Map.of(
      CPT_0, new ProcessingTime(40, MINUTES.getName()),
      CPT_1, new ProcessingTime(50, MINUTES.getName()),
      CPT_2, new ProcessingTime(50, MINUTES.getName()),
      CPT_3, new ProcessingTime(60, MINUTES.getName())
  );

  private static final List<PlanningDistributionResponse> FORECAST = List.of(
      forecastedBacklog(OPERATING_HOUR_0, CPT_0, 100),
      forecastedBacklog(OPERATING_HOUR_1, CPT_1, 250),
      forecastedBacklog(OPERATING_HOUR_1, CPT_2, 300),
      forecastedBacklog(OPERATING_HOUR_2, CPT_3, 180)
  );

  private static final List<ProjectionResult> PROJECTION_RESULTS = List.of(
      projectionResult(CPT_0, CPT_0_RESULT),
      projectionResult(CPT_1, CPT_1_RESULT),
      projectionResult(CPT_2, CPT_2_RESULT),
      projectionResult(CPT_3, CPT_3_RESULT)
  );

  @InjectMocks
  private SaveSimulationOutbound saveSimulationOutbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private GetWaveSuggestion getWaveSuggestion;

  @Mock
  private GetEntities getEntities;

  @Mock
  private GetSimpleDeferralProjection getSimpleDeferralProjection;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Mock
  private GetSales getSales;

  @Mock
  private FeatureSwitches featureSwitches;

  @Mock
  private CalculateProjectionService calculateProjection;

  @Mock
  private ProjectionGateway projectionGateway;

  @Mock
  private RatioService ratioService;

  private static ProjectionResult projectionResult(final Instant cpt, final Instant projectedEndDate) {
    return ProjectionResult.builder()
        .date(ZonedDateTime.ofInstant(cpt, UTC))
        .projectedEndDate(projectedEndDate == null ? null : ZonedDateTime.ofInstant(projectedEndDate, UTC))
        .remainingQuantity(0)
        .build();
  }

  private static PlanningDistributionResponse forecastedBacklog(final Instant dateIn, final Instant dateOut, final long quantity) {
    return PlanningDistributionResponse.builder()
        .dateIn(ZonedDateTime.ofInstant(dateIn, UTC))
        .dateOut(ZonedDateTime.ofInstant(dateOut, UTC))
        .total(quantity)
        .build();
  }

  private static Photo.Group photoGroup(final Step step, final Instant dateOut, final String area, final int total) {
    return new Photo.Group(
        Map.of(
            STEP, step.getName(),
            DATE_OUT, dateOut.toString(),
            AREA, area
        ),
        total,
        total
    );
  }

  private static MagnitudePhoto throughputResponse(final ProcessName process, final Instant date, final int value) {
    return MagnitudePhoto.builder()
        .processName(process)
        .date(ZonedDateTime.ofInstant(date, UTC))
        .value(value)
        .build();
  }

  private static Map<Instant, Integer> throughputs(final int op0, final int op1, final int op2, final int op3) {
    return Map.of(
        OPERATING_HOUR_0, op0,
        OPERATING_HOUR_1, op1,
        OPERATING_HOUR_2, op2,
        OPERATING_HOUR_3, op3
    );
  }

  private static Stream<MagnitudePhoto> throughputs(final ProcessName process, final int op0, final int op1, final int op2, final int op3) {
    return Stream.of(
        throughputResponse(process, OPERATING_HOUR_0, op0),
        throughputResponse(process, OPERATING_HOUR_1, op1),
        throughputResponse(process, OPERATING_HOUR_2, op2),
        throughputResponse(process, OPERATING_HOUR_3, op3)
    );
  }

  private static ZonedDateTime getCurrentTime() {
    return now(UTC).withMinute(0).withSecond(0).withNano(0);
  }

  @BeforeEach
  void setUp() {
    final ZonedDateTime utcCurrentTime = getCurrentTime();
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusHours(2);

    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(utcCurrentTime);

    final List<String> steps = List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group",
        "grouping", "grouped", "to_pack");

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(getWaveSuggestion.execute((GetWaveSuggestionInputDto.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .zoneId(TIME_ZONE.toZoneId())
            .date(utcDateTimeFrom)
            .build()
        )
    )).thenReturn(mockSuggestedWaves(utcDateTimeFrom, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(new GetProjectionInput(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND,
        utcDateTimeFrom,
        mockBacklog(),
        false,
        emptyList())))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjections(utcCurrentTime),
            new LogisticCenterConfiguration(TIME_ZONE)));

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        List.of("outbound-orders"),
        steps,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(4).toInstant(),
        List.of("date_out"))
    ).thenReturn(List.of(
        new Consolidation(null, Map.of("date_out", getCurrentTime().minusHours(1).toString()), 150, true),
        new Consolidation(null, Map.of("date_out", getCurrentTime().plusHours(2).toString()), 235, true),
        new Consolidation(null, Map.of("date_out", getCurrentTime().plusHours(3).toString()), 300, true)
    ));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedPlanningBacklog);
  }

  @Test
  @DisplayName("Execute the case when all the data is correctly generated")
  void testExecute() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentTime();
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);

    final List<ProcessName> processes = List.of(PACKING, PACKING_WALL);
    final List<Backlog> mockedBacklog = mockBacklog();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(false);

    when(planningModelGateway.saveSimulation(createSimulationRequest(mockedBacklog, utcCurrentTime, processes)))
        .thenReturn(mockProjections(utcCurrentTime));

    // When
    final PlanningView planningView = saveSimulationOutbound.execute(GetProjectionInputDto.builder()
        .date(utcDateTimeFrom)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .simulations(List.of(new Simulation(PICKING,
            List.of(new SimulationEntity(
                HEADCOUNT, List.of(
                new QuantityByDate(
                    utcCurrentTime, 20))
            )))))
        .requestDate(utcCurrentTime.toInstant())
        .build()
    );

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertProjection(planningView.getData().getProjections());

    verifyNoInteractions(calculateProjection);
    verifyNoInteractions(projectionGateway);
    verifyNoInteractions(ratioService);
  }

  @Test
  void testWhenLibFeatureToggleIsActiveThenSimulationsAreStoredAndProjectionsCalledWithSavedSimulations() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentTime();
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.plusHours(1);

    final var dateFrom = utcCurrentTime;
    final var dateTo = utcCurrentTime.plusDays(4);

    final var dateFromAsInstant = dateFrom.toInstant();
    final var dateToAsInstant = dateTo.toInstant();

    final var simulations = List.of(
        new Simulation(
            PICKING,
            List.of(
                new SimulationEntity(
                    HEADCOUNT,
                    List.of(new QuantityByDate(utcCurrentTime, 20))
                )
            )
        )
    );

    final var input = GetProjectionInputDto.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .simulations(simulations)
        .date(utcDateTimeFrom)
        .requestDate(utcCurrentTime.toInstant())
        .userId(USER_ID)
        .build();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(true);

    when(backlogGateway.getLastPhoto(
        new BacklogLastPhotoRequest(
            input.getWarehouseId(),
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            getSteps(FBM_WMS_OUTBOUND),
            null,
            null,
            dateFromAsInstant,
            dateToAsInstant,
            Set.of(STEP, DATE_OUT, AREA),
            input.getRequestDate()
        ))
    ).thenReturn(new Photo(dateFromAsInstant, PHOTO_GROUPS));

    when(planningModelGateway.getPlanningDistribution(Mockito.argThat(request ->
            WAREHOUSE_ID.equals(request.getWarehouseId())
                && WORKFLOW.equals(request.getWorkflow())
                && dateFrom.equals(request.getDateInFrom())
                && dateTo.equals(request.getDateInTo())
                && dateFrom.equals(request.getDateOutFrom())
                && dateTo.equals(request.getDateOutTo())
                && request.isApplyDeviation()
        ))
    ).thenReturn(FORECAST);

    final var slas = List.of(
        ZonedDateTime.ofInstant(CPT_0, UTC),
        ZonedDateTime.ofInstant(CPT_1, UTC),
        ZonedDateTime.ofInstant(CPT_2, UTC),
        ZonedDateTime.ofInstant(CPT_3, UTC)
    );
    when(planningModelGateway.getCycleTime(
        input.getWarehouseId(),
        CycleTimeRequest.builder()
            .workflows(Set.of(FBM_WMS_OUTBOUND))
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .slas(slas)
            .timeZone(BA_ZONE)
            .build()
    )).thenReturn(Map.of(FBM_WMS_OUTBOUND, CYCLE_TIMES));

    when(ratioService.getPackingRatio(
        input.getWarehouseId(),
        input.getRequestDate(),
        dateTo.toInstant().plus(2, HOURS),
        dateFrom.toInstant(),
        dateTo.toInstant()
    )).thenReturn(RATIOS);

    final var processNames = List.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);
    final var entityTypes = List.of(THROUGHPUT);
    when(planningModelGateway.searchTrajectories(Mockito.argThat(request ->
            WAREHOUSE_ID.equals(request.getWarehouseId())
                && WORKFLOW.equals(request.getWorkflow())
                && processNames.equals(request.getProcessName())
                && entityTypes.equals(request.getEntityTypes())
                && dateFrom.equals(request.getDateFrom())
                && dateTo.equals(request.getDateTo())
                && SIMULATION.equals(request.getSource())
        ))
    ).thenReturn(Map.of(THROUGHPUT, THROUGHPUT_AT_PROCESS));

    when(calculateProjection.execute(
        input.getRequestDate(),
        dateFromAsInstant,
        dateToAsInstant,
        FBM_WMS_OUTBOUND,
        THROUGHPUT_PER_PROCESS,
        PHOTO_GROUPS,
        FORECAST,
        PROCESSING_TIMES,
        RATIOS
    )).thenReturn(PROJECTION_RESULTS);

    // When
    final PlanningView planningView = saveSimulationOutbound.execute(input);

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertProjections(planningView.getData().getProjections());

    verify(projectionGateway).deferralSaveSimulation(
        new SaveSimulationsRequest(FBM_WMS_OUTBOUND, WAREHOUSE_ID, simulations, USER_ID)
    );
    verify(planningModelGateway, never()).saveSimulation(any());
  }

  private void assertProjections(final List<Projection> projections) {
    assertEquals(4, projections.size());
    assertEquals(CPT_0, projections.get(3).getCpt());
    assertEquals(CPT_0_RESULT, projections.get(3).getProjectedEndDate());

    assertEquals(CPT_1, projections.get(2).getCpt());
    assertEquals(CPT_1_RESULT, projections.get(2).getProjectedEndDate());

    assertEquals(CPT_2, projections.get(1).getCpt());
    assertEquals(CPT_2_RESULT, projections.get(1).getProjectedEndDate());

    assertEquals(CPT_3, projections.get(0).getCpt());
    assertEquals(CPT_3_RESULT, projections.get(0).getProjectedEndDate());
  }

  private void assertProjection(final List<Projection> projections) {
    final ZonedDateTime currentTime = getCurrentUtcDate();

    final Projection projection1 = projections.get(0);
    final ZonedDateTime cpt1 = currentTime.plusHours(8);
    assertProjectionData(projection1, cpt1, null);

    final Projection projection2 = projections.get(1);
    final ZonedDateTime cpt2 = currentTime.plusHours(7);
    final ZonedDateTime projectedEndDate2 = currentTime.plusHours(3);
    assertProjectionData(projection2, cpt2, projectedEndDate2);

    final Projection projection3 = projections.get(2);
    final ZonedDateTime cpt3 = currentTime.plusHours(6);
    final ZonedDateTime projectedEndDate3 = currentTime.plusHours(8).plusMinutes(10);
    assertProjectionData(projection3, cpt3, projectedEndDate3);

    final Projection projection4 = projections.get(3);
    final ZonedDateTime cpt4 = currentTime.plusHours(5).plusMinutes(30);
    final ZonedDateTime projectedEndDate4 = currentTime.plusHours(3).plusMinutes(25);
    assertProjectionData(projection4, cpt4, projectedEndDate4);

    final Projection projection5 = projections.get(4);
    final ZonedDateTime cpt5 = currentTime.plusHours(4);
    final ZonedDateTime projectedEndDate5 = currentTime.plusHours(2).plusMinutes(30);
    assertProjectionData(projection5, cpt5, projectedEndDate5);
  }

  private void assertProjectionData(final Projection projection,
                                    final ZonedDateTime cpt,
                                    final ZonedDateTime projectedEndDate) {
    final Instant projectEnd = projectedEndDate != null ? projectedEndDate.toInstant() : null;

    assertEquals(cpt.toInstant(), projection.getCpt());
    assertEquals(projectEnd, projection.getProjectedEndDate());
  }

  private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
    return List.of(
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(4))
            .projectedEndDate(utcCurrentTime.plusHours(2).plusMinutes(30))
            .simulatedEndDate(utcCurrentTime.plusHours(2).plusMinutes(35))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(7))
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .simulatedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(30)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(5).plusMinutes(30))
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
            .remainingQuantity(50)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(6))
            .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
            .simulatedEndDate(utcCurrentTime.plusHours(8).plusMinutes(11))
            .remainingQuantity(180)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(8))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(300, MINUTES.getName()))
            .build()
    );
  }

  private SimulationRequest createSimulationRequest(final List<Backlog> backlogs,
                                                    final ZonedDateTime currentTime,
                                                    final List<ProcessName> processes) {
    return SimulationRequest.builder()
        .processName(processes)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(currentTime)
        .dateTo(currentTime.plusDays(4))
        .backlog(backlogs.stream()
            .map(backlog -> new QuantityByDate(
                backlog.getDate(),
                backlog.getQuantity()))
            .collect(toList()))
        .simulations(List.of(new Simulation(PICKING,
            List.of(new SimulationEntity(
                HEADCOUNT, List.of(new QuantityByDate(currentTime, 20))
            )))))
        .applyDeviation(true)
        .timeZone("America/Argentina/Buenos_Aires")
        .build();
  }

  private List<Backlog> mockBacklog() {
    final ZonedDateTime currentTime = getCurrentTime();

    return List.of(
        new Backlog(currentTime.minusHours(1), 150),
        new Backlog(currentTime.plusHours(2), 235),
        new Backlog(currentTime.plusHours(3), 300)
    );
  }

  private SimpleTable mockSuggestedWaves(final ZonedDateTime utcDateTimeFrom,
                                         final ZonedDateTime utcDateTimeTo) {
    final String title = "Ondas sugeridas";
    final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER) + "-"
        + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER);
    final String expectedTitle = "Sig. hora " + nextHour;
    final List<ColumnHeader> columnHeaders = List.of(
        new ColumnHeader("column_1", expectedTitle, null),
        new ColumnHeader("column_2", "Tamaño de onda", null)
    );
    final List<Map<String, Object>> data = List.of(
        Map.of("column_1",
            Map.of("title", "Unidades por onda", "subtitle",
                MONO_ORDER_DISTRIBUTION.getTitle()),
            "column_2", "0 uds."
        ),
        Map.of("column_1",
            Map.of("title", "Unidades por onda", "subtitle",
                MULTI_BATCH_DISTRIBUTION.getTitle()),
            "column_2", "100 uds."
        ),
        Map.of("column_1",
            Map.of("title", "Unidades por onda", "subtitle",
                MULTI_ORDER_DISTRIBUTION.getTitle()),
            "column_2", "100 uds."
        )
    );
    return new SimpleTable(title, columnHeaders, data);
  }

  private ComplexTable mockComplexTable() {
    return new ComplexTable(
        emptyList(),
        emptyList(),
        new ComplexTableAction("applyLabel", "cancelLabel", "editLabel"),
        "title"
    );
  }
}
