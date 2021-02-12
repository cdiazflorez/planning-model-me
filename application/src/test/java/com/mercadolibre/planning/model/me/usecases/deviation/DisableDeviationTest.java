package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DisableDeviationTest {

    @InjectMocks
    private DisableDeviation disableDeviation;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Test
    void testExecuteOk() {
        // GIVEN
        final DisableDeviationInput disableDeviationInput =
                new DisableDeviationInput(WAREHOUSE_ID,FBM_WMS_OUTBOUND);

        when(planningModelGateway.disableDeviation(disableDeviationInput))
                .thenReturn(DeviationResponse.builder()
                        .status(200)
                        .build());

        // WHEN
        final DeviationResponse deviationResponse = disableDeviation.execute(disableDeviationInput);

        // THEN
        assertNotNull(deviationResponse);
        assertEquals(200, deviationResponse.getStatus());
        assertEquals("Forecast deviation disabled", deviationResponse.getMessage());
    }

}
