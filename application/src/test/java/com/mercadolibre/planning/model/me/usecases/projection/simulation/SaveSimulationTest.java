package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
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
public class SaveSimulationTest {

  private static final ZonedDateTime CURRENT_DATE = Instant.parse("2022-09-09T17:00:00Z").atZone(ZoneId.of("UTC"));

  @InjectMocks
  SaveSimulation saveSimulation;

  @Mock
  SaveSimulationInbound saveSimulationInbound;

  @Mock
  SaveSimulationOutbound saveSimulationOutbound;

  @Test
  public void executeTest() {
    //GIVEN
    final GetProjectionInputDto simulationInputOutbound = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .date(CURRENT_DATE)
        .build();

    final GetProjectionInputDto simulationInputInbound = GetProjectionInputDto.builder()
        .workflow(FBM_WMS_INBOUND)
        .date(CURRENT_DATE)
        .build();

    final PlanningView mockPlanning = PlanningView.builder()
        .isNewVersion(true)
        .currentDate(CURRENT_DATE)
        .build();

    when(saveSimulationInbound.execute(simulationInputInbound)).thenReturn(mockPlanning);
    when(saveSimulationOutbound.execute(simulationInputOutbound)).thenReturn(mockPlanning);

    //WHEN
    PlanningView outboundResponse = saveSimulation.execute(simulationInputInbound);
    PlanningView inboundResponse = saveSimulation.execute(simulationInputOutbound);

    //THEN
    assertEquals(mockPlanning, outboundResponse);
    assertEquals(mockPlanning, inboundResponse);
  }

}
