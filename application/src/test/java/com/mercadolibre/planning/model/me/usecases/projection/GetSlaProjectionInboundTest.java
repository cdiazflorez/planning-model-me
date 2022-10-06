package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.TIME_ZONE;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockComplexTable;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ResultData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.Date;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)

@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionInboundTest {
  private static final ZonedDateTime SLA_1 = getCurrentUtcDate().minusHours(2);
  private static final ZonedDateTime SLA_2 = getCurrentUtcDate().minusHours(1);
  private static final ZonedDateTime SLA_3 = getCurrentUtcDate().plusHours(4);
  private static final ZonedDateTime SLA_4 = getCurrentUtcDate().plusHours(5);
  private static final ZonedDateTime SLA_5 = getCurrentUtcDate().plusHours(5).plusMinutes(30);
  private static final ZonedDateTime SLA_6 = getCurrentUtcDate().plusHours(6);
  private static final ZonedDateTime SLA_7 = getCurrentUtcDate().plusHours(7);
  private static final ZonedDateTime SLA_8 = getCurrentUtcDate();
  private static final ZonedDateTime SLA_9 = getCurrentUtcDate().plusDays(2);

  @InjectMocks
  private GetSlaProjectionInbound getSlaProjectionInbound;

  @Mock
  private PlanningModelGateway planningModelGateway;


  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private GetEntities getEntities;

  @Mock
  private GetSales getSales;

  @Mock
  private FeatureSwitches featureSwitches;

  @Mock
  private GetProjection getProjection;

  @Mock
  private GetBacklogByDateInbound getBacklogByDateInbound;

  @Test
  void testInboundExecute() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(1).plusHours(1);
    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog();
    final List<Backlog> mockedBacklog = List.of(
        new Backlog(SLA_1, 50),
        new Backlog(SLA_2, -30),
        new Backlog(SLA_3, 150),
        new Backlog(SLA_4, 235),
        new Backlog(SLA_5, 300),
        new Backlog(SLA_6, 120)
    );

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_INBOUND)
        .warehouseId(WAREHOUSE_ID)
        .requestDate(Instant.now())
        .date(null)
        .build();


    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(getBacklogByDateInbound.execute(new GetBacklogByDateDto(FBM_WMS_INBOUND, WAREHOUSE_ID,
        currentUtcDateTime.toInstant(), utcDateTimeTo.toInstant())))
        .thenReturn(mockedBacklog);

    when(planningModelGateway.runProjection(
        createProjectionRequestInbound(mockedPlanningBacklog, currentUtcDateTime, utcDateTimeTo)))
        .thenReturn(mockProjectionResults(currentUtcDateTime));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getProjection.execute(any(GetProjectionInputDto.class))).thenReturn(mockPlanningView());

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    when(featureSwitches.shouldCallBacklogApi()).thenReturn(true);

    // When
    final PlanningView projection = getSlaProjectionInbound.execute(input);

    // Then
    assertNull(projection.getEmptyStateMessage());
    assertProjection(projection.getData().getProjections(), true);
    assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
  }

  @Test
  void testInboundSecondDayExecute() {
    // Given
    final ZonedDateTime currentUtcDateTime = getCurrentUtcDate();
    final ZonedDateTime utcDateTimeTo = currentUtcDateTime.plusDays(3);
    final ZonedDateTime utcDateTimeFrom = currentUtcDateTime.minusDays(1);

    final List<Backlog> mockedBacklog = List.of(
        new Backlog(SLA_8, 50),
        new Backlog(SLA_2, -30),
        new Backlog(SLA_3, 150),
        new Backlog(SLA_4, 235),
        new Backlog(SLA_5, 300),
        new Backlog(SLA_6, 120),
        new Backlog(SLA_9, 60)
    );

    final GetProjectionInputDto input = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_INBOUND)
        .warehouseId(WAREHOUSE_ID)
        .date(currentUtcDateTime)
        .requestDate(Instant.now().truncatedTo(ChronoUnit.NANOS).minus(1, ChronoUnit.DAYS))
        .build();


    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(getBacklogByDateInbound.execute(new GetBacklogByDateDto(FBM_WMS_INBOUND, WAREHOUSE_ID,
        utcDateTimeFrom.toInstant(), utcDateTimeTo.toInstant())))
        .thenReturn(mockedBacklog);

    when(planningModelGateway.runProjection(any()))
        .thenReturn(mockProjectionResults(currentUtcDateTime));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getProjection.execute(any(GetProjectionInputDto.class))).thenReturn(mockPlanningView());

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedBacklog);

    when(featureSwitches.shouldCallBacklogApi()).thenReturn(true);

    // When
    final PlanningView projection = getSlaProjectionInbound.execute(input);

    // Then
    assertNull(projection.getEmptyStateMessage());
    assertProjection(projection.getData().getProjections(), false);
    assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
  }

  private List<Backlog> mockPlanningBacklog() {
    final ZonedDateTime truncatedDate = convertToTimeZone(TIME_ZONE.toZoneId(), SLA_1)
        .truncatedTo(ChronoUnit.DAYS);

    final ZonedDateTime sla1 = convertToTimeZone(ZoneId.of("Z"), truncatedDate);

    return List.of(
        new Backlog(sla1, 50),
        new Backlog(SLA_3, 150),
        new Backlog(SLA_4, 235),
        new Backlog(SLA_5, 300),
        new Backlog(SLA_6, 120)
    );
  }

  private ProjectionRequest createProjectionRequestInbound(final List<Backlog> backlogs,
                                                           final ZonedDateTime dateFrom,
                                                           final ZonedDateTime dateTo) {
    return ProjectionRequest.builder()
        .processName(List.of(CHECK_IN, PUT_AWAY))
        .workflow(FBM_WMS_INBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .type(ProjectionType.CPT)
        .backlog(backlogs)
        .applyDeviation(true)
        .timeZone("America/Argentina/Buenos_Aires")
        .build();
  }

  private ProjectionResult mockProjectionResult(final ZonedDateTime date,
                                                final ZonedDateTime projectedEndDate,
                                                final int remainingQuantity) {

    return ProjectionResult.builder()
        .date(date)
        .projectedEndDate(projectedEndDate)
        .remainingQuantity(remainingQuantity)
        .processingTime(new ProcessingTime(0, MINUTES.getName()))
        .build();
  }

  private List<ProjectionResult> mockProjectionResults(final ZonedDateTime utcCurrentTime) {
    return List.of(
        mockProjectionResult(SLA_3, utcCurrentTime.plusHours(3).plusMinutes(30), 0),
        mockProjectionResult(SLA_4, utcCurrentTime.plusHours(3), 0),
        mockProjectionResult(SLA_5, utcCurrentTime.plusHours(3).plusMinutes(25), 100),
        mockProjectionResult(SLA_6, utcCurrentTime.plusHours(8).plusMinutes(10), 180),
        mockProjectionResult(SLA_7, utcCurrentTime.plusHours(3), 100),
        mockProjectionResult(SLA_1, utcCurrentTime.plusMinutes(15), 100)
    );
  }

  private PlanningView mockPlanningView() {
    final Date[] dates = {new Date("", "", true)};

    return PlanningView.builder()
        .isNewVersion(true)
        .currentDate(getCurrentUtcDate())
        .dateSelector(new DateSelector("title Date", dates))
        .data(new ResultData(
            null,
            mockComplexTable(),
            emptyList()
        ))
        .build();
  }

  private void assertProjection(final List<Projection> projections, final boolean isFirstDay) {
    final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

    assertEquals(isFirstDay ? 6 : 5, projections.size());

    final Projection projection1 = projections.get(0);
    final ZonedDateTime projectedEndDate1 = utcCurrentTime.plusHours(3);
    assertProjectionData(projection1, SLA_7, projectedEndDate1, 100);

    final Projection projection2 = projections.get(1);
    final ZonedDateTime projectedEndDate2 = utcCurrentTime.plusHours(8).plusMinutes(10);
    assertProjectionData(projection2, SLA_6, projectedEndDate2, 180);

    final Projection projection3 = projections.get(2);
    final ZonedDateTime projectedEndDate3 = utcCurrentTime.plusHours(3).plusMinutes(25);
    assertProjectionData(projection3, SLA_5, projectedEndDate3, 100);

    final Projection projection4 = projections.get(3);
    final ZonedDateTime projectedEndDate4 = utcCurrentTime.plusHours(3);
    assertProjectionData(projection4, SLA_4, projectedEndDate4, 0);

    final Projection projection5 = projections.get(4);
    final ZonedDateTime projectedEndDate5 = utcCurrentTime.plusHours(3).plusMinutes(30);
    assertProjectionData(projection5, SLA_3, projectedEndDate5, 0);

    if (isFirstDay) {
      final Projection projection6 = projections.get(5);
      final ZonedDateTime projectedEndDate6 = utcCurrentTime.plusMinutes(15);
      assertProjectionData(projection6, SLA_1, projectedEndDate6, 100);
    }
  }

  private void assertProjectionData(final Projection projection,
                                    final ZonedDateTime cpt,
                                    final ZonedDateTime projectedEndDate,
                                    final int remainingQuantity) {

    assertEquals(cpt.toInstant(), projection.getCpt());
    assertEquals(0, projection.getCycleTime());
    assertEquals(remainingQuantity, projection.getRemainingQuantity());
    assertEquals(projectedEndDate.toInstant(), projection.getProjectedEndDate());
  }
}
