package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Cardinality.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.projection.ProjectionWorkflow.getSteps;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SlaProperties;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.GetWaveSuggestion;
import com.mercadolibre.planning.model.me.usecases.wavesuggestion.dto.GetWaveSuggestionInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link GetSlaProjectionOutbound}.
 */
@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionOutboundTest {

  private static final List<String> STATUSES = of("pending", "to_route", "to_pick", "picked", "to_sort",
      "sorted", "to_group", "grouping", "grouped", "to_pack");

  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

  private static final ZonedDateTime CPT_1 = getCurrentUtcDate().plusHours(4);

  private static final ZonedDateTime CPT_2 = getCurrentUtcDate().plusHours(5);

  private static final ZonedDateTime CPT_3 = getCurrentUtcDate().plusHours(5).plusMinutes(30);

  private static final ZonedDateTime CPT_4 = getCurrentUtcDate().plusHours(6);

  private static final ZonedDateTime CPT_5 = getCurrentUtcDate().plusHours(7);

  @InjectMocks
  private GetSlaProjectionOutbound getSlaProjectionOutbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private GetWaveSuggestion getWaveSuggestion;

  @Mock
  private GetEntities getEntities;

  @Mock
  private GetSales getSales;

  @Mock
  private GetSimpleDeferralProjection getSimpleDeferralProjection;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Mock
  private FeatureSwitches featureSwitches;

  @Mock
  private CalculateProjectionService calculateProjection;

  @Mock
  private RatioService ratioService;

  /*
  @Test
  void testOutboundExecute() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(1);

    final List<ProcessName> processes = of(PACKING, PACKING_WALL);
    final List<Integer> processingTimes = of(300, 250, 240, 240, 45);

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(currentUtcDateTime)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(false);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    when(planningModelGateway.runProjection(
        createProjectionRequestOutbound(mockedBacklog, processes, currentUtcDateTime, utcDateTimeTo))
    ).thenReturn(mockProjections(currentUtcDateTime));

    when(getWaveSuggestion.execute(any(GetWaveSuggestionInputDto.class)))
        .thenReturn(mockSuggestedWaves(currentUtcDateTime, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionsDeferral(currentUtcDateTime),
            new LogisticCenterConfiguration(TIME_ZONE)));

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        of("outbound-orders"),
        STATUSES,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(1).toInstant(),
        of("date_out"))
    ).thenReturn(of(
        new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
        new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
        new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true),
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)
    ));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertProjection(planningView.getData().getProjections(), processingTimes);
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertSimpleTable(planningView.getData().getSimpleTable1(), currentUtcDateTime, utcDateTimeTo);
  }

  @Test
  void testOutboundExecuteEnabled() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.truncatedTo(HOURS);
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(2);

    final var slaFrom = now().truncatedTo(HOURS).toInstant();
    final var slaTo = slaFrom.plus(4, ChronoUnit.DAYS);
    final var photoDate = slaTo.plus(30, ChronoUnit.MINUTES);

    final List<Backlog> mockedBacklog = mockBacklog();

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .warehouseId(WAREHOUSE_ID)
        .requestDate(currentUtcDateTime.toInstant())
        .date(utcDateTimeTo)
        .workflow(FBM_WMS_OUTBOUND)
        .build();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(true);

    when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(new Photo(Instant.now(), of()));

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(new LogisticCenterConfiguration(TIME_ZONE, true));

    when(planningModelGateway.getCycleTime(eq(WAREHOUSE_ID), any()))
        .thenReturn(
            Map.of(FBM_WMS_OUTBOUND,
                Map.of(
                    CPT_1.toInstant(), new SlaProperties(60),
                    CPT_2.toInstant(), new SlaProperties(60),
                    CPT_3.toInstant(), new SlaProperties(60),
                    CPT_4.toInstant(), new SlaProperties(60)
                )
            )
        );

    when(getWaveSuggestion.execute(any(GetWaveSuggestionInputDto.class))).thenReturn(mockSuggestedWaves(currentUtcDateTime, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionsDeferral(currentUtcDateTime),
            new LogisticCenterConfiguration(TIME_ZONE)));


    when(backlogGateway.getLastPhoto(new BacklogLastPhotoRequest(
                WAREHOUSE_ID,
                Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
                getSteps(FBM_WMS_OUTBOUND),
                null,
                null,
                slaFrom,
                slaTo,
                Set.of(STEP, DATE_OUT, AREA),
                slaTo
            )
        )
    ).thenReturn(
        generatePhoto(photoDate)
    );

    when(ratioService.getPackingRatio(
            eq(WAREHOUSE_ID),
            any(),
            any(),
            any(),
            any()
        )
    ).thenReturn(
        generatePackingRatioByHour(Instant.from(currentUtcDateTime), Instant.from(utcDateTimeTo), 0.5, 0.5)
    );

    when(planningModelGateway.searchTrajectories(any()))
        .thenReturn(generateMagnitudesPhoto(currentUtcDateTime, utcDateTimeTo));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    when(planningModelGateway.getPlanningDistribution(Mockito.any())
    ).thenReturn(emptyList());

    //result of calculateProjection isn't real, because it trys test the before algorithm. NO TEST THIS OUTPUT
    when(calculateProjection.execute(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any())
    ).thenReturn(projectionResults());

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertSimpleTable(planningView.getData().getSimpleTable1(), currentUtcDateTime, utcDateTimeTo);
  }

  @Test
  void testOutboundExecuteEnabledInFCWithoutWall() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.truncatedTo(HOURS);
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(1);

    final var slaFrom = now().truncatedTo(HOURS).toInstant();
    final var slaTo = slaFrom.plus(1, ChronoUnit.DAYS);
    final var photoDate = slaTo.plus(30, ChronoUnit.MINUTES);

    final List<Backlog> mockedBacklog = mockBacklog();

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .warehouseId(WAREHOUSE_ID)
        .requestDate(currentUtcDateTime.toInstant())
        .date(utcDateTimeTo)
        .workflow(FBM_WMS_OUTBOUND)
        .build();

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(true);

    when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class))).thenReturn(new Photo(Instant.now(), of()));

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(new LogisticCenterConfiguration(TIME_ZONE, false));

    when(planningModelGateway.getCycleTime(eq(WAREHOUSE_ID), any()))
        .thenReturn(
            Map.of(FBM_WMS_OUTBOUND,
                Map.of(
                    CPT_1.toInstant(), new SlaProperties(60),
                    CPT_2.toInstant(), new SlaProperties(60),
                    CPT_3.toInstant(), new SlaProperties(60),
                    CPT_4.toInstant(), new SlaProperties(60)
                )
            )
        );

    when(getWaveSuggestion.execute(any(GetWaveSuggestionInputDto.class))).thenReturn(mockSuggestedWaves(currentUtcDateTime, utcDateTimeTo));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSimpleDeferralProjection.execute(any(GetProjectionInput.class)))
        .thenReturn(new GetSimpleDeferralProjectionOutput(
            mockProjectionsDeferral(currentUtcDateTime),
            new LogisticCenterConfiguration(TIME_ZONE)));


    when(backlogGateway.getLastPhoto(new BacklogLastPhotoRequest(
                WAREHOUSE_ID,
                Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
                getSteps(FBM_WMS_OUTBOUND),
                null,
                null,
                slaFrom,
                slaTo,
                Set.of(STEP, DATE_OUT, AREA),
                slaTo
            )
        )
    ).thenReturn(
        generatePhoto(photoDate)
    );

    when(planningModelGateway.searchTrajectories(any()))
        .thenReturn(generateMagnitudesPhoto(currentUtcDateTime, utcDateTimeTo));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    when(planningModelGateway.getPlanningDistribution(any())
    ).thenReturn(emptyList());

    //result of calculateProjection isn't real, because it trys test the before algorithm. NO TEST THIS OUTPUT
    when(calculateProjection.execute(
        Instant.from(currentUtcDateTime),
        Instant.from(utcDateTimeFrom),
        Instant.from(utcDateTimeTo),
        FBM_WMS_OUTBOUND,
        generateThroughput(generateMagnitudesPhoto(currentUtcDateTime, utcDateTimeTo).get(THROUGHPUT)),
        generatePhoto(photoDate).getGroups(),
        emptyList(),
        Map.of(
            CPT_1.toInstant(), new ProcessingTime(60, MINUTES.getName()),
            CPT_2.toInstant(), new ProcessingTime(60, MINUTES.getName()),
            CPT_3.toInstant(), new ProcessingTime(60, MINUTES.getName()),
            CPT_4.toInstant(), new ProcessingTime(60, MINUTES.getName())
        ),
        generatePackingRatioByHour(currentUtcDateTime.toInstant(), utcDateTimeTo.toInstant(), 1.0, 0.0))
    ).thenReturn(projectionResults());

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getEmptyStateMessage());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertSimpleTable(planningView.getData().getSimpleTable1(), currentUtcDateTime, utcDateTimeTo);
  }
   */

  @Test
  void testExecuteWithError() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime;
    final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusDays(4);
    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(utcDateTimeFrom)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    final List<Backlog> mockedBacklog = mockBacklog();

    final List<ProcessName> processes = of(PICKING, PACKING, PACKING_WALL);
    when(planningModelGateway.runProjection(
        createProjectionRequestOutbound(mockedBacklog, processes, utcDateTimeFrom, utcDateTimeTo)))
        .thenThrow(RuntimeException.class);

    when(backlogGateway.getCurrentBacklog(
        WAREHOUSE_ID,
        of("outbound-orders"),
        STATUSES,
        now().truncatedTo(ChronoUnit.HOURS).toInstant(),
        now().truncatedTo(ChronoUnit.HOURS).plusDays(1).plusHours(1).toInstant(),
        of("date_out"))
    ).thenReturn(of(
        new Consolidation(null, Map.of("date_out", CPT_1.toString()), 150, true),
        new Consolidation(null, Map.of("date_out", CPT_2.toString()), 235, true),
        new Consolidation(null, Map.of("date_out", CPT_3.toString()), 300, true),
        new Consolidation(null, Map.of("date_out", CPT_4.toString()), 120, true)));

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getData());
  }

  private Photo generatePhoto(final Instant photoDate) {
    return new Photo(
        photoDate,
        of(
            new Photo.Group(
                Map.of(DATE_OUT, CPT_1.toString()),
                150,
                0
            ),
            new Photo.Group(
                Map.of(DATE_OUT, CPT_2.toString()),
                235,
                0
            ),
            new Photo.Group(
                Map.of(DATE_OUT, CPT_3.toString()),
                300,
                0
            ),
            new Photo.Group(
                Map.of(DATE_OUT, CPT_4.toString()),
                120,
                0
            )
        )
    );
  }

  private Map<ProcessName, Map<Instant, Integer>> generateThroughput(final List<MagnitudePhoto> magnitudes) {
    return magnitudes.stream().collect(Collectors.groupingBy(MagnitudePhoto::getProcessName,
        Collectors.toMap(
            entry -> entry.getDate().toInstant(),
            MagnitudePhoto::getValue)));
  }

  private Map<MagnitudeType, List<MagnitudePhoto>> generateMagnitudesPhoto(final ZonedDateTime currentDate, final ZonedDateTime dateTo) {

    ZonedDateTime date = currentDate.truncatedTo(HOURS);
    final List<MagnitudePhoto> magnitudesPhoto = new ArrayList<>();
    final var processNames = of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      ZonedDateTime finalDate = date;
      processNames.forEach(processName -> magnitudesPhoto.add(
          MagnitudePhoto.builder()
              .date(finalDate)
              .value(1000)
              .processName(processName)
              .build()));

      date = date.plusHours(1);
    }

    return Map.of(THROUGHPUT, magnitudesPhoto);
  }

  private Map<Instant, PackingRatioCalculator.PackingRatio> generatePackingRatioByHour(
      final Instant currentDate,
      final Instant dateTo,
      final Double packingToteRatio,
      final Double packingWallRatio) {
    Instant date = currentDate.truncatedTo(HOURS);
    final TreeMap<Instant, PackingRatioCalculator.PackingRatio> ratioByHour = new TreeMap<>();

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      ratioByHour.put(date, new PackingRatioCalculator.PackingRatio(packingToteRatio, packingWallRatio));

      date = date.plus(1, HOURS);
    }

    return ratioByHour;
  }

  private void assertProjection(final List<Projection> projections,
                                final List<Integer> processingTimes) {
    final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

    assertEquals(5, projections.size());

    final Projection projection1 = projections.get(0);
    final ZonedDateTime cpt1 = CPT_1.plusHours(3);
    assertProjectionData(projection1, cpt1, null, processingTimes.get(0), 100);

    final Projection projection2 = projections.get(1);
    final ZonedDateTime cpt2 = CPT_2.plusHours(1);
    final ZonedDateTime projectedEndDate2 = utcCurrentTime.plusHours(8).plusMinutes(10);
    assertProjectionData(projection2, cpt2, projectedEndDate2, processingTimes.get(1), 180);

    final Projection projection3 = projections.get(2);
    final ZonedDateTime projectedEndDate3 = utcCurrentTime.plusHours(3).plusMinutes(25);
    assertProjectionData(projection3, CPT_3, projectedEndDate3, processingTimes.get(2), 100);

    final Projection projection4 = projections.get(3);
    final ZonedDateTime cpt4 = CPT_4.minusHours(1);
    final ZonedDateTime projectedEndDate4 = utcCurrentTime.plusHours(3);
    assertProjectionData(projection4, cpt4, projectedEndDate4, processingTimes.get(3), 0);

    final Projection projection5 = projections.get(4);
    final ZonedDateTime cpt5 = CPT_5.minusHours(3);
    final ZonedDateTime projectedEndDate5 = utcCurrentTime.plusHours(3).plusMinutes(30);
    assertProjectionData(projection5, cpt5, projectedEndDate5, processingTimes.get(4), 0);
  }

  private void assertProjectionData(final Projection projection,
                                    final ZonedDateTime cpt,
                                    final ZonedDateTime projectedEndDate,
                                    final int processingTime,
                                    final int remainingQuantity) {
    final Instant endDate = projectedEndDate != null ? projectedEndDate.toInstant() : null;

    assertEquals(cpt.toInstant(), projection.getCpt());
    assertEquals(processingTime, projection.getCycleTime());
    assertEquals(remainingQuantity, projection.getRemainingQuantity());
    assertEquals(endDate, projection.getProjectedEndDate());
  }

  private void assertSimpleTable(final SimpleTable simpleTable,
                                 final ZonedDateTime utcDateTimeFrom,
                                 final ZonedDateTime utcDateTimeTo) {
    List<Map<String, Object>> data = simpleTable.getData();
    assertEquals(3, data.size());
    assertEquals("0 uds.", data.get(0).get("column_2"));
    final Map<String, Object> column1Mono = (Map<String, Object>) data.get(0).get("column_1");
    assertEquals(MONO_ORDER_DISTRIBUTION.getTitle(), column1Mono.get("subtitle"));

    assertEquals("100 uds.", data.get(1).get("column_2"));
    final Map<String, Object> column1Multi = (Map<String, Object>) data.get(1).get("column_1");
    assertEquals(MULTI_BATCH_DISTRIBUTION.getTitle(), column1Multi.get("subtitle"));

    assertEquals("100 uds.", data.get(1).get("column_2"));
    final Map<String, Object> column1MultiBatch = (Map<String, Object>) data.get(2).get("column_1");
    assertEquals(MULTI_ORDER_DISTRIBUTION.getTitle(), column1MultiBatch.get("subtitle"));

    final String title = simpleTable.getColumns().get(0).getTitle();
    final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER) + "-"
        + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER);
    final String expectedTitle = "Sig. hora " + nextHour;
    assertEquals(title, expectedTitle);
  }

  private ProjectionRequest createProjectionRequestOutbound(final List<Backlog> backlogs,
                                                            final List<ProcessName> processes,
                                                            final ZonedDateTime dateFrom,
                                                            final ZonedDateTime dateTo) {
    return ProjectionRequest.builder()
        .processName(processes)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .type(ProjectionType.CPT)
        .backlog(backlogs)
        .applyDeviation(true)
        .timeZone(BA_ZONE)
        .build();
  }

  private List<ProjectionResult> mockProjections(ZonedDateTime utcCurrentTime) {
    return of(
        ProjectionResult.builder()
            .date(CPT_1)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(45, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_2)
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_3)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_4)
            .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
            .remainingQuantity(180)
            .processingTime(new ProcessingTime(250, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_5)
            .projectedEndDate(null)
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(300, MINUTES.getName()))
            .isDeferred(true)
            .build()
    );
  }

  private List<ProjectionResult> mockProjectionsDeferral(ZonedDateTime utcCurrentTime) {
    return of(
        ProjectionResult.builder()
            .date(CPT_1)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(30))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(45, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_2)
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_3)
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .isDeferred(false)
            .build(),
        ProjectionResult.builder()
            .date(CPT_4)
            .projectedEndDate(utcCurrentTime.plusHours(8).plusMinutes(10))
            .remainingQuantity(180)
            .processingTime(new ProcessingTime(250, MINUTES.getName()))
            .isDeferred(false)
            .build()
    );
  }

  private List<Backlog> mockBacklog() {
    return of(
        new Backlog(CPT_1, 150),
        new Backlog(CPT_2, 235),
        new Backlog(CPT_3, 300),
        new Backlog(CPT_4, 120)
    );
  }

  private SimpleTable mockSuggestedWaves(final ZonedDateTime utcDateTimeFrom,
                                         final ZonedDateTime utcDateTimeTo) {
    final String title = "Ondas sugeridas";
    final String nextHour = utcDateTimeFrom.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER) + "-"
        + utcDateTimeTo.withZoneSameInstant(TIME_ZONE.toZoneId())
        .format(HOUR_MINUTES_FORMATTER);
    final String expextedTitle = "Sig. hora " + nextHour;
    final List<ColumnHeader> columnHeaders = of(
        new ColumnHeader("column_1", expextedTitle, null),
        new ColumnHeader("column_2", "Tamaño de onda", null)
    );
    final List<Map<String, Object>> data = of(
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

  private List<ProjectionResult> projectionResults() {
    return of(
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_1.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            415,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            false,
            false,
            null,
            0,
            null
        ),
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_2.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            500,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            false,
            false,
            null,
            0,
            null
        ),
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_3.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            950,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            false,
            false,
            null,
            0,
            null
        )
    );
  }
}
