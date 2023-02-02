package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.BA_ZONE;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.CPT_1;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.CPT_2;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.CPT_3;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.TIME_ZONE;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.generateMockMagnitudesPhoto;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.generateMockPackingRatioByHour;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.generateMockPhoto;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.generateMockThroughput;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.mockExpectedBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.OutboundProjectionTestUtils.mockProjectionResults;
import static com.mercadolibre.planning.model.me.usecases.projection.ProjectionWorkflow.getSteps;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WORKFLOW;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.CycleTimeRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SlaProperties;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjectionOutput;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  @InjectMocks
  private GetSlaProjectionOutbound getSlaProjectionOutbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private CalculateProjectionService calculateProjection;

  @Mock

  private GetSimpleDeferralProjection getSimpleDeferralProjection;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private BacklogApiGateway backlogGateway;

  @Test
  void testExecute() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime;
    final ZonedDateTime utcDateTimeTo = utcDateTimeFrom.plusDays(4);

    final List<Backlog> mockedBacklog = mockBacklog();

    final Instant photoDate = utcDateTimeTo.plus(30, ChronoUnit.MINUTES).toInstant();
    final GetProjectionInputDto getProjectionInputDto = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(utcDateTimeFrom)
        .requestDate(currentUtcDateTime.toInstant())
        .build();

    final var currentBacklog = generateMockPhoto(photoDate).getGroups();
    final var logisticCenterConfiguration = new LogisticCenterConfiguration(TIME_ZONE);
    final List<ProjectionResult> projectionResults = mockProjectionResults();
    final List<PlanningDistributionResponse> expectedBacklog = mockExpectedBacklog();
    final Map<Instant, PackingRatioCalculator.PackingRatio> packingRatioByHour = generateMockPackingRatioByHour(
        currentUtcDateTime.toInstant(),
        utcDateTimeTo.toInstant()
    );

    when(backlogGateway.getLastPhoto(new BacklogLastPhotoRequest(
                WAREHOUSE_ID,
                Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
                getSteps(FBM_WMS_OUTBOUND),
                null,
                null,
                utcDateTimeFrom.toInstant(),
                utcDateTimeTo.toInstant(),
                Set.of(STEP, DATE_OUT, AREA),
                utcDateTimeTo.toInstant()
            )
        )
    ).thenReturn(
        generateMockPhoto(photoDate)
    );

    when(getSimpleDeferralProjection.execute(
        new GetProjectionInput(
            getProjectionInputDto.getWarehouseId(),
            getProjectionInputDto.getWorkflow(),
            getProjectionInputDto.getDate(),
            mockedBacklog,
            false,
            emptyList()
        )
    )).thenReturn(new GetSimpleDeferralProjectionOutput(projectionResults, logisticCenterConfiguration));

    when(planningModelGateway.getPlanningDistribution(Mockito.argThat(request ->
            WAREHOUSE_ID.equals(request.getWarehouseId())
                && WORKFLOW.equals(request.getWorkflow())
                && currentUtcDateTime.equals(request.getDateInFrom())
                && utcDateTimeTo.equals(request.getDateInTo())
                && currentUtcDateTime.equals(request.getDateOutFrom())
                && utcDateTimeTo.equals(request.getDateOutTo())
                && request.isApplyDeviation()
        ))
    ).thenReturn(expectedBacklog);

    when(planningModelGateway.getCycleTime(WAREHOUSE_ID, CycleTimeRequest.builder()
        .workflows(Set.of(FBM_WMS_OUTBOUND))
        .dateFrom(currentUtcDateTime)
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

    when(planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .processName(of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL))
        .entityTypes(of(THROUGHPUT))
        .dateFrom(currentUtcDateTime)
        .dateTo(utcDateTimeTo)
        .source(SIMULATION)
        .simulations(emptyList())
        .build()))
        .thenReturn(generateMockMagnitudesPhoto(currentUtcDateTime, utcDateTimeTo));

    when(calculateProjection.execute(
            currentUtcDateTime.toInstant(),
            currentUtcDateTime.toInstant(),
            utcDateTimeTo.toInstant(),
            WORKFLOW,
            generateMockThroughput(generateMockMagnitudesPhoto(currentUtcDateTime, utcDateTimeTo).get(THROUGHPUT)),
            currentBacklog,
            emptyList(),
            new HashMap<>(Map.of(
                CPT_1.toInstant(), new ProcessingTime(60, MINUTES.getName()),
                CPT_2.toInstant(), new ProcessingTime(60, MINUTES.getName()),
                CPT_3.toInstant(), new ProcessingTime(60, MINUTES.getName())
            )),
            packingRatioByHour
        )
    ).thenReturn(projectionResults);


    // When
    final List<ProjectionResult> planningView = getSlaProjectionOutbound.getProjection(
        getProjectionInputDto,
        utcDateTimeFrom,
        utcDateTimeTo,
        mockedBacklog,
        new LogisticCenterConfiguration(TIME_ZONE)
    );

    // Then
    assertNotNull(planningView);
  }

  @Test
  @Deprecated
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

    // When
    final PlanningView planningView = getSlaProjectionOutbound.execute(input);

    // Then
    assertNull(planningView.getData());
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


  private List<Backlog> mockBacklog() {
    return of(
        new Backlog(CPT_1, 150),
        new Backlog(CPT_2, 235),
        new Backlog(CPT_3, 300)
    );
  }
}
