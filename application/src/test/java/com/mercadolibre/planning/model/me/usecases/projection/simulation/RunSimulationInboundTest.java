package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.TIME_ZONE;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockBacklog;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockComplexTable;
import static com.mercadolibre.planning.model.me.usecases.projection.InboundProjectionTestUtils.mockPlanningBacklog;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
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
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
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
  private GetSales getSales;

  @Mock
  private GetBacklogByDateInbound getBacklog;

  @Test
  public void testExecuteInbound() {
    // Given
    final ZonedDateTime utcCurrentTime = getCurrentUtcDate();

    final List<Backlog> mockedBacklog = mockBacklog(utcCurrentTime);
    final List<Backlog> mockedPlanningBacklog = mockPlanningBacklog(utcCurrentTime);

    when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID))
        .thenReturn(new LogisticCenterConfiguration(TIME_ZONE));

    when(planningModelGateway.runSimulation(createSimulationRequest(mockedPlanningBacklog, utcCurrentTime)))
        .thenReturn(mockProjections(utcCurrentTime));

    when(getEntities.execute(any(GetProjectionInputDto.class))).thenReturn(mockComplexTable());

    when(getBacklog.execute(
        new GetBacklogByDateDto(
            FBM_WMS_INBOUND,
            WAREHOUSE_ID,
            utcCurrentTime.toInstant(),
            utcCurrentTime.plusDays(1).plusHours(1).toInstant()
        )))
        .thenReturn(mockedBacklog);

    when(getSales.execute(any(GetSalesInputDto.class))).thenReturn(mockedPlanningBacklog);

    // When
    final PlanningView planningView = runSimulationInbound.execute(GetProjectionInputDto.builder()
        .date(utcCurrentTime)
        .workflow(FBM_WMS_INBOUND)
        .warehouseId(WAREHOUSE_ID)
        .simulations(List.of(new Simulation(PUT_AWAY, List.of(new SimulationEntity(
            HEADCOUNT, List.of(new QuantityByDate(utcCurrentTime, 20))
        )))))
        .requestDate(Instant.now())
        .build()
    );


    // Then
    assertNull(planningView.getEmptyStateMessage());

    assertEquals(5, planningView.getData().getProjections().size());
    assertEquals(mockComplexTable(), planningView.getData().getComplexTable1());
    assertProjection(planningView.getData().getProjections());

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
        .dateTo(currentTime.plusDays(1).plusHours(1))
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
