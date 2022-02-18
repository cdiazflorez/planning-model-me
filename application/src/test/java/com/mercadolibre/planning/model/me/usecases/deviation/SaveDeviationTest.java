package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaveDeviationTest {

    @InjectMocks
    private SaveDeviation saveDeviation;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Test
    void testExecuteOk() {
        // GIVEN
        final SaveDeviationInput saveDeviationInput = SaveDeviationInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(now())
                .dateTo(now().plusDays(1))
                .value(5.9)
                .userId(USER_ID)
                .build();

        when(planningModelGateway.saveDeviation(any(SaveDeviationInput.class)))
                .thenReturn(DeviationResponse.builder()
                        .status(200)
                        .build());

        // WHEN
        final DeviationResponse deviationResponse = saveDeviation.execute(saveDeviationInput);

        // THEN
        assertNotNull(deviationResponse);
        assertEquals(200, deviationResponse.getStatus());
        assertEquals("Forecast deviation saved", deviationResponse.getMessage());
    }

}
