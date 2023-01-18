package com.mercadolibre.planning.model.me.usecases.deviation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisableDeviationTest {

  @InjectMocks
  private DisableDeviation disableDeviation;

  @Mock
  private DeviationGateway planningModelGateway;

  @Test
  void testExecuteOk() {
    // GIVEN
    final DisableDeviationInput disableDeviationInput =
        new DisableDeviationInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND);

    when(planningModelGateway.disableDeviation(disableDeviationInput))
        .thenReturn(DeviationResponse.builder()
            .status(200)
            .build());

    // WHEN
    assertDoesNotThrow(() -> disableDeviation.execute(disableDeviationInput));
  }

}
