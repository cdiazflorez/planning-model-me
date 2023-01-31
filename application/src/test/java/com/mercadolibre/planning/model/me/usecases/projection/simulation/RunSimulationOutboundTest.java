package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockPlanningBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.ProjectionWorkflow.getSteps;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.CycleTimeRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SlaProperties;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
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
 * Tests {@link RunSimulationOutbound}.
 */
@ExtendWith(MockitoExtension.class)
public class RunSimulationOutboundTest {
  private static final String BA_ZONE = "America/Argentina/Buenos_Aires";

  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(BA_ZONE);

  private static final ZonedDateTime UTC_CURRENT_DATE = now(UTC).withMinute(0).withSecond(0).withNano(0);

  private static final ZonedDateTime CPT_1 = UTC_CURRENT_DATE.minusHours(1);

  private static final ZonedDateTime CPT_2 = UTC_CURRENT_DATE.plusHours(2);

  private static final ZonedDateTime CPT_3 = UTC_CURRENT_DATE.plusHours(3);

  private static final List<ProjectionResult> PROJECTION_RESULTS = projectionResults();

  @InjectMocks
  private RunSimulationOutbound runSimulationOutbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

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
  private RatioService ratioService;

  @Mock
  private CalculateProjectionService calculateProjection;

  private static List<ProjectionResult> projectionResults() {
    return of(
        new ProjectionResult(
            ZonedDateTime.ofInstant(CPT_1.toInstant(), ZoneId.of("UTC")),
            null,
            null,
            415,
            new ProcessingTime(10, ChronoUnit.MINUTES.toString()),
            true,
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

  @Test
  public void testExecute() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentTime();
    final List<Backlog> mockedBacklog = mockBacklog(utcCurrentTime);
    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(utcCurrentTime);
    final List<ProcessName> processes = List.of(PACKING, PACKING_WALL);

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(false);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(planningModelGateway.runSimulation(
        createSimulationRequest(mockedBacklog, utcCurrentTime, processes)))
        .thenReturn(mockProjections(utcCurrentTime));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(backlogGateway.getLastPhoto(any(BacklogLastPhotoRequest.class)))
        .thenReturn(generatePhoto(Instant.now()));

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedPlanningBacklog);

    // When
    final PlanningView planningView = runSimulationOutbound.execute(GetProjectionInputDto.builder()
        .date(utcCurrentTime)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .simulations(generateSimulation())
        .requestDate(utcCurrentTime.toInstant())
        .build()
    );

    // Then
    assertEquals(5, planningView.getData().getProjections().size());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertProjection(planningView.getData().getProjections());
  }

  @Test
  public void testExecuteEnabled() {
    // Given
    final List<String> steps = List.of("pending", "to_route", "to_pick", "picked", "to_sort", "sorted", "to_group",
        "grouping", "grouped", "to_pack");

    final List<Backlog> mockedBacklog = mockBacklog(UTC_CURRENT_DATE);
    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(UTC_CURRENT_DATE);

    when(featureSwitches.isProjectionLibEnabled(WAREHOUSE_ID)).thenReturn(true);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE, true));

    final ZonedDateTime utcDateTimeTo = UTC_CURRENT_DATE.plusDays(1).plusHours(1);

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedPlanningBacklog);

    when(planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .processName(of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL))
        .entityTypes(of(THROUGHPUT))
        .dateFrom(UTC_CURRENT_DATE)
        .dateTo(utcDateTimeTo)
        .source(SIMULATION)
        .simulations(emptyList())
        .build()))
        .thenReturn(generateMagnitudesPhoto(UTC_CURRENT_DATE, utcDateTimeTo));

    when(planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .processName(of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL))
        .entityTypes(of(THROUGHPUT))
        .dateFrom(UTC_CURRENT_DATE)
        .dateTo(utcDateTimeTo)
        .source(SIMULATION)
        .simulations(generateSimulation())
        .build()))
        .thenReturn(generateMagnitudesPhoto(UTC_CURRENT_DATE, utcDateTimeTo));

    when(planningModelGateway.getCycleTime(WAREHOUSE_ID, CycleTimeRequest.builder()
        .workflows(Set.of(FBM_WMS_OUTBOUND))
        .dateFrom(UTC_CURRENT_DATE)
        .dateTo(utcDateTimeTo)
        .slas(of(CPT_1, CPT_2, CPT_3))
        .timeZone(BA_ZONE)
        .build()))
        .thenReturn(
            Map.of(FBM_WMS_OUTBOUND,
                Map.of(
                    CPT_1.toInstant(), new SlaProperties(60),
                    CPT_2.toInstant(), new SlaProperties(60),
                    CPT_3.toInstant(), new SlaProperties(60)
                )
            )
        );

    final var slaFrom = now().truncatedTo(HOURS).toInstant();
    final var slaTo = slaFrom.plus(1, ChronoUnit.DAYS).plus(1, HOURS);
    final var photoDate = slaTo.plus(30, ChronoUnit.MINUTES);

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

    when(backlogGateway.getLastPhoto(new BacklogLastPhotoRequest(
                WAREHOUSE_ID,
                Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
                getSteps(FBM_WMS_OUTBOUND),
                null,
                null,
                slaFrom,
                slaTo,
                Set.of(DATE_OUT),
                slaTo
            )
        )
    ).thenReturn(
        generatePhoto(photoDate)
    );

    when(ratioService.getPackingRatio(
            WAREHOUSE_ID,
            Instant.from(UTC_CURRENT_DATE),
            slaTo.plus(2, HOURS),
            slaFrom,
            slaTo
        )
    ).thenReturn(
        generatePackingRatioByHour(Instant.from(UTC_CURRENT_DATE), Instant.from(utcDateTimeTo))
    );

    when(planningModelGateway.getPlanningDistribution(Mockito.argThat(request ->
            WAREHOUSE_ID.equals(request.getWarehouseId())
                && WORKFLOW.equals(request.getWorkflow())
                && UTC_CURRENT_DATE.equals(request.getDateInFrom())
                && utcDateTimeTo.equals(request.getDateInTo())
                && UTC_CURRENT_DATE.equals(request.getDateOutFrom())
                && utcDateTimeTo.equals(request.getDateOutTo())
                && request.isApplyDeviation()
        ))
    ).thenReturn(emptyList());

    //result of calculateProjection isn't real, because it trys test the before algorithm. NO TEST THIS OUTPUT
    when(calculateProjection.execute(
        Instant.from(UTC_CURRENT_DATE),
        Instant.from(UTC_CURRENT_DATE),
        Instant.from(utcDateTimeTo),
        FBM_WMS_OUTBOUND,
        generateThroughput(generateMagnitudesPhoto(UTC_CURRENT_DATE, utcDateTimeTo).get(THROUGHPUT)),
        generatePhoto(photoDate).getGroups(),
        emptyList(),
        new TreeMap<>(Map.of(
            CPT_1.toInstant(), new ProcessingTime(60, MINUTES.getName()),
            CPT_2.toInstant(), new ProcessingTime(60, MINUTES.getName()),
            CPT_3.toInstant(), new ProcessingTime(60, MINUTES.getName())
        )),
        generatePackingRatioByHour(UTC_CURRENT_DATE.toInstant(), utcDateTimeTo.toInstant()))
    ).thenReturn(PROJECTION_RESULTS);

    // When
    final PlanningView planningView =
        runSimulationOutbound.execute(GetProjectionInputDto.builder()
            .date(UTC_CURRENT_DATE)
            .workflow(FBM_WMS_OUTBOUND)
            .warehouseId(WAREHOUSE_ID)
            .simulations(generateSimulation())
            .requestDate(UTC_CURRENT_DATE.toInstant())
            .build()
        );

    // Then
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
  }

  private void assertProjection(final List<Projection> projections) {
    final ZonedDateTime currentTime = getCurrentUtcDate();

    final Projection projection1 = projections.get(0);
    final ZonedDateTime cpt1 = currentTime.plusHours(7);
    assertProjectionData(projection1, cpt1, null);

    final Projection projection2 = projections.get(1);
    final ZonedDateTime cpt2 = currentTime.plusHours(6);
    final ZonedDateTime projectedEndDate2 = currentTime.plusHours(8).plusMinutes(10);
    assertProjectionData(projection2, cpt2, projectedEndDate2);

    final Projection projection3 = projections.get(2);
    final ZonedDateTime cpt3 = currentTime.plusHours(5).plusMinutes(30);
    final ZonedDateTime projectedEndDate3 = currentTime.plusHours(3).plusMinutes(25);
    assertProjectionData(projection3, cpt3, projectedEndDate3);

    final Projection projection4 = projections.get(3);
    final ZonedDateTime cpt4 = currentTime.plusHours(5);
    final ZonedDateTime projectedEndDate4 = currentTime.plusHours(3);
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
            .date(utcCurrentTime.plusHours(5))
            .projectedEndDate(utcCurrentTime.plusHours(3))
            .simulatedEndDate(utcCurrentTime.plusHours(3))
            .remainingQuantity(0)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
            .build(),
        ProjectionResult.builder()
            .date(utcCurrentTime.plusHours(5).plusMinutes(30))
            .projectedEndDate(utcCurrentTime.plusHours(3).plusMinutes(25))
            .simulatedEndDate(utcCurrentTime.plusHours(3).plusMinutes(20))
            .remainingQuantity(0)
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
            .date(utcCurrentTime.plusHours(7))
            .remainingQuantity(100)
            .processingTime(new ProcessingTime(240, MINUTES.getName()))
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
        .dateTo(currentTime.plusDays(1).plusHours(1))
        .backlog(backlogs.stream()
            .map(backlog -> new QuantityByDate(
                backlog.getDate(),
                backlog.getQuantity()))
            .collect(toList()))
        .simulations(List.of(new Simulation(PICKING, List.of(new SimulationEntity(
            HEADCOUNT, List.of(new QuantityByDate(currentTime, 20))
        )))))
        .applyDeviation(true)
        .timeZone(BA_ZONE)
        .build();
  }

  private ZonedDateTime getCurrentTime() {
    return now(UTC).withMinute(0).withSecond(0).withNano(0);
  }

  private ComplexTable mockComplexTable() {
    return new ComplexTable(
        emptyList(),
        emptyList(),
        new ComplexTableAction("applyLabel", "cancelLabel", "editLabel"),
        "title"
    );
  }

  private List<Simulation> generateSimulation() {
    return of(new Simulation(PICKING, of(new SimulationEntity(
        HEADCOUNT, of(new QuantityByDate(UTC_CURRENT_DATE, 20))
    ))));
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
            )
        )
    );
  }

  private Map<Instant, PackingRatioCalculator.PackingRatio> generatePackingRatioByHour(final Instant currentDate, final Instant dateTo) {
    Instant date = currentDate.truncatedTo(HOURS);
    final TreeMap<Instant, PackingRatioCalculator.PackingRatio> ratioByHour = new TreeMap<>();

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      ratioByHour.put(date, new PackingRatioCalculator.PackingRatio(0.5, 0.5));

      date = date.plus(1, HOURS);
    }

    return ratioByHour;
  }

  private Map<ProcessName, Map<Instant, Integer>> generateThroughput(final List<MagnitudePhoto> magnitudes) {
    return magnitudes.stream().collect(Collectors.groupingBy(MagnitudePhoto::getProcessName,
        Collectors.toMap(
            entry -> entry.getDate().toInstant(),
            MagnitudePhoto::getValue)));
  }
}
