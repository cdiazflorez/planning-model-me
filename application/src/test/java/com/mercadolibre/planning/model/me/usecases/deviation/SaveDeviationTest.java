package com.mercadolibre.planning.model.me.usecases.deviation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveDeviationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveDeviationTest {
  private static final ZonedDateTime DATE_FROM = ZonedDateTime.parse("2021-01-21T15:00Z[UTC]");

  private static final ZonedDateTime DATE_TO = ZonedDateTime.parse("2021-01-21T17:00Z[UTC]");

  private static final List<SaveDeviationInput> SAVE_DEVIATION_INPUTS = List.of(
      SaveDeviationInput.builder()
          .warehouseId(WAREHOUSE_ID)
          .workflow(INBOUND)
          .paths(List.of(ShipmentType.SPD, ShipmentType.PRIVATE))
          .dateFrom(DATE_FROM)
          .dateTo(DATE_TO)
          .type(DeviationType.UNITS)
          .value(0.1)
          .userId(USER_ID)
          .build(),
      SaveDeviationInput.builder()
          .warehouseId(WAREHOUSE_ID)
          .workflow(INBOUND_TRANSFER)
          .dateFrom(DATE_FROM)
          .dateTo(DATE_TO)
          .type(DeviationType.UNITS)
          .value(0.1)
          .userId(USER_ID)
          .build()
  );

  @InjectMocks
  private SaveDeviation saveDeviation;

  @Mock
  private PlanningModelGateway planningModelGateway;

  @Mock
  private DeviationGateway deviationGateway;

  @Test
  void testExecuteOkInbound() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_INBOUND, 5.9);

    // WHEN
    saveDeviation.execute(saveDeviationInput);

    // THEN
    verify(planningModelGateway).newSaveDeviation(saveDeviationInput);
  }

  @Test
  void testExecuteOkOutbound() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_OUTBOUND, 5.9);

    // WHEN
    saveDeviation.execute(saveDeviationInput);

    // THEN
    verify(planningModelGateway).newSaveDeviation(saveDeviationInput);
  }

  @Test
  void testExecuteValueGreaterThanRange() {
    // GIVEN
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_INBOUND, 120.00);

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
    final SaveDeviationInput saveDeviationInput = givenSaveDeviationInput(FBM_WMS_INBOUND, -120.00);

    // WHEN
    final IllegalArgumentException deviationResponse = assertThrows(
        IllegalArgumentException.class,
        () -> saveDeviation.execute(saveDeviationInput)
    );

    // THEN
    assertEquals("The value must be between -100 to 100", deviationResponse.getMessage());
  }

  @Test
  void testSaveOk() {
    // WHEN
    saveDeviation.save(SAVE_DEVIATION_INPUTS);

    // THEN
    verify(deviationGateway).save(SAVE_DEVIATION_INPUTS);
    verifyNoInteractions(planningModelGateway);
  }

  private SaveDeviationInput givenSaveDeviationInput(
          final Workflow warehouse,
          final Double value
  ) {
    return SaveDeviationInput.builder()
        .workflow(warehouse)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .type(DeviationType.UNITS)
        .value(value)
        .userId(USER_ID)
        .build();
  }
}
