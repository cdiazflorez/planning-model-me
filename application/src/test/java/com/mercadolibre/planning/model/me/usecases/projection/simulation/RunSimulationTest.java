package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RunSimulationTest {

  private static final ZonedDateTime CURRENT_DATE = Instant.parse("2022-08-17T09:00:00Z").atZone(ZoneId.of("UTC"));

  @InjectMocks
  RunSimulation runSimulation;

  @Mock
  RunSimulationInbound runSimulationInbound;

  @Mock
  RunSimulationOutbound runSimulationOutbound;

  @Test
  public void executeTest() {
    //GIVEN
    final GetProjectionInputDto projectionInputOutbound = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .date(CURRENT_DATE)
        .build();

    final GetProjectionInputDto projectionInputInbound = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_INBOUND)
        .date(CURRENT_DATE)
        .build();

    when(runSimulationOutbound.execute(projectionInputOutbound)).thenReturn(mockPlanningView());
    when(runSimulationInbound.execute(projectionInputInbound)).thenReturn(mockPlanningView());

    //WHEN
    PlanningView outboundResponse = runSimulation.execute(projectionInputOutbound);
    PlanningView inboundResponse = runSimulation.execute(projectionInputInbound);

    //THEN
    assertEquals(mockPlanningView(), outboundResponse);
    assertEquals(mockPlanningView(), inboundResponse);


  }

  private PlanningView mockPlanningView() {

    return PlanningView.builder()
        .isNewVersion(true)
        .currentDate(CURRENT_DATE)
        .build();
  }
}
