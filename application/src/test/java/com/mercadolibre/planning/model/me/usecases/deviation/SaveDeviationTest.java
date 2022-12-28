package com.mercadolibre.planning.model.me.usecases.deviation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SaveDeviationTest {

  @InjectMocks
  private SaveDeviation saveDeviation;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Test
  void testExecuteOkInbound() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS, 5.9);

    // WHEN
    saveDeviation.execute(saveDeviationInput);

    // THEN
    verify(planningModelGateway).newSaveDeviation(saveDeviationInput);
  }

  @Test
  void testExecuteOkOutbound() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_OUTBOUND, DeviationType.UNITS, 5.9);

    // WHEN
    saveDeviation.execute(saveDeviationInput);

    // THEN
    verify(planningModelGateway).newSaveDeviation(saveDeviationInput);
  }

  @Test
  void testExecuteValueGreaterThanRange() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS, 120.00);

    // WHEN
    final IllegalArgumentException deviationResponse = assertThrows(
        IllegalArgumentException.class,
        () -> saveDeviation.execute(saveDeviationInput)
    );

    // THEN
    assertEquals("The value must be between -100 to 100", deviationResponse.getMessage());
  }

  @Test
  void testExecuteValueLessThanRange() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_INBOUND, DeviationType.UNITS, -120.00);

    // WHEN
    final IllegalArgumentException deviationResponse = assertThrows(
        IllegalArgumentException.class,
        () -> saveDeviation.execute(saveDeviationInput)
    );

    // THEN
    assertEquals("The value must be between -100 to 100", deviationResponse.getMessage());
  }

  private SaveDeviationInput givenSaveDeviationInput(
      final Workflow warehouse,
      final DeviationType deviationType,
      final Double value) {
    return SaveDeviationInput.builder()
        .workflow(warehouse)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .type(deviationType)
        .value(value)
        .userId(USER_ID)
        .build();
  }
}
