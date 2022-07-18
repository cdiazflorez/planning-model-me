package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WriteSimulationTest {

    @InjectMocks
    private WriteSimulation writeSimulation;

    @Mock
    private ProjectionGateway projectionGateway;

    @Test
    public void saveSimulationsTest() {

        doNothing().when(projectionGateway).deferralSaveSimulation(any());

        writeSimulation.saveSimulations(Workflow.FBM_WMS_OUTBOUND, "ARTW01", Collections.emptyList(), 1L);

        verify(projectionGateway, times(1)).deferralSaveSimulation(any());
    }
}
