package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.DATE_FORMATTER;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.DATE_SHORT_FORMATTER;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.TIME_ZONE;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockComplexTable;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockPlanningBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockSimpleTable;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogByDateInbound;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogByDateDto;
import com.mercadolibre.planning.model.me.usecases.projection.GetEntities;
import com.mercadolibre.planning.model.me.usecases.projection.GetProjectionSummary;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionSummaryInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RunSimulationInboundTest {

  @InjectMocks
  private RunSimulationInbound runSimulationInbound;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private GetEntities getEntities;

  @Mock
  private GetProjectionSummary getProjectionSummary;

  @Mock
  private GetBacklogByDateInbound getBacklog;

  @Test
  public void testExecuteInbound() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentUtcDate();
    final ZonedDateTime currentTime = utcCurrentTime.withZoneSameInstant(TIME_ZONE.toZoneId());

    final List<Backlog> mockedBacklog = mockBacklog(utcCurrentTime);
    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(utcCurrentTime);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(getBacklog.execute(
        new GetBacklogByDateDto(
            FBM_WMS_INBOUND,
            WAREHOUSE_ID,
            utcCurrentTime.toInstant(),
            utcCurrentTime.plusDays(4).toInstant()
        )))
        .thenReturn(mockedBacklog);

    when(planningModelGateway.runSimulation(createSimulationRequest(mockedPlanningBacklog, utcCurrentTime)))
        .thenReturn(mockProjections(utcCurrentTime));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getProjectionSummary.execute(any(GetProjectionSummaryInput.class)))
        .thenReturn(mockSimpleTable());

    // When
    final Projection projection = runSimulationInbound.execute(GetProjectionInputDto.builder()
                                                                   .date(utcCurrentTime)
                                                                   .workflow(FBM_WMS_INBOUND)
                                                                   .warehouseId(WAREHOUSE_ID)
                                                                   .simulations(
                                                                       List.of(new Simulation(PUT_AWAY, List.of(new SimulationEntity(
                                                                           HEADCOUNT, List.of(new QuantityByDate(utcCurrentTime, 20))
                                                                       )))))
                                                                   .requestDate(Instant.now())
                                                                   .build()
    );

    // Then
    assertNull(projection.getEmptyStateMessage());

    assertEquals("Proyecciones", projection.getTitle());

    final Chart chart = projection.getData().getChart();
    final List<ChartData> chartData = chart.getData();

    assertEquals(5, chartData.size());

    final ChartData chartData1 = chartData.get(0);
    assertEquals(currentTime.plusHours(4).format(DATE_SHORT_FORMATTER),
                 chartData1.getTitle());
    assertEquals(currentTime.plusHours(4).format(DATE_FORMATTER), chartData1.getCpt());
    assertEquals(currentTime.plusHours(2).plusMinutes(35).format(DATE_FORMATTER),
                 chartData1.getProjectedEndTime());

    final ChartData chartData2 = chartData.get(1);
    assertEquals(currentTime.plusHours(5).format(DATE_SHORT_FORMATTER),
                 chartData2.getTitle());
    assertEquals(currentTime.plusHours(5).format(DATE_FORMATTER), chartData2.getCpt());
    assertEquals(currentTime.plusHours(3).format(DATE_FORMATTER),
                 chartData2.getProjectedEndTime());

    final ChartData chartData3 = chartData.get(2);
    assertEquals(
        currentTime.plusHours(5).plusMinutes(30)
            .format(DATE_SHORT_FORMATTER),
        chartData3.getTitle()
    );
    assertEquals(currentTime.plusHours(5).plusMinutes(30).format(DATE_FORMATTER),
                 chartData3.getCpt());
    assertEquals(currentTime.plusHours(3).plusMinutes(20).format(DATE_FORMATTER),
                 chartData3.getProjectedEndTime());

    final ChartData chartData4 = chartData.get(3);
    assertEquals(currentTime.plusHours(6).format(DATE_SHORT_FORMATTER),
                 chartData4.getTitle()
    );
    assertEquals(currentTime.plusHours(6).format(DATE_FORMATTER), chartData4.getCpt());
    assertEquals(currentTime.plusHours(8).plusMinutes(11).format(DATE_FORMATTER),
                 chartData4.getProjectedEndTime());

    final ChartData chartData5 = chartData.get(4);
    assertEquals(currentTime.plusHours(7).format(DATE_SHORT_FORMATTER),
                 chartData5.getTitle()
    );
    assertEquals(currentTime.plusHours(7).format(DATE_FORMATTER), chartData5.getCpt());
    assertEquals(currentTime.plusDays(1).format(DATE_FORMATTER),
                 chartData5.getProjectedEndTime());

    assertEquals(mockComplexTable(), projection.getData().getComplexTable1());
    assertEquals(mockSimpleTable(), projection.getData().getSimpleTable2());
  }

  private List<ProjectionResult> mockProjections(final ZonedDateTime utcCurrentTime) {
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
                                                    final ZonedDateTime currentTime) {
    return SimulationRequest.builder()
        .processName(List.of(CHECK_IN, PUT_AWAY))
        .workflow(FBM_WMS_INBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(currentTime)
        .dateTo(currentTime.plusDays(4))
        .backlog(backlogs.stream()
                     .map(backlog -> new QuantityByDate(
                         backlog.getDate(),
                         backlog.getQuantity()))
                     .collect(toList()))
        .simulations(List.of(new Simulation(PUT_AWAY, List.of(new SimulationEntity(
            HEADCOUNT, List.of(new QuantityByDate(currentTime, 20))
        )))))
        .applyDeviation(true)
        .timeZone("America/Argentina/Buenos_Aires")
        .build();
  }
}
