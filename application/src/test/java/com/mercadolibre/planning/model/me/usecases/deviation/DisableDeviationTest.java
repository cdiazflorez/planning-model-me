package com.mercadolibre.planning.model.me.usecases.deviation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisableDeviationTest {
  private static final List<ShipmentType> AFFECTED_SHIPMENT_TYPES = List.of(
      ShipmentType.SPD,
      ShipmentType.PRIVATE
  );

  @InjectMocks
  private DisableDeviation disableDeviation;

  @Mock
  private DeviationGateway planningModelGateway;

  @Test
  void testExecuteOk() {
    // GIVEN
    final DisableDeviationInput disableDeviationInput = DisableDeviationInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .build();

    when(planningModelGateway.disableDeviation(disableDeviationInput))
        .thenReturn(DeviationResponse.builder()
            .status(200)
            .build());

    // WHEN
    assertDoesNotThrow(() -> disableDeviation.execute(disableDeviationInput));
  }

  @Test
  void testExecuteAllMinutesOk() {
    // WHEN
    final List<DisableDeviationInput> disableDeviationInput = generateDisableDeviationInput(DeviationType.MINUTES, AFFECTED_SHIPMENT_TYPES);

    disableDeviation.executeAll(WAREHOUSE_ID, disableDeviationInput);

    // THEN
    verify(planningModelGateway).disableDeviationAll(eq(WAREHOUSE_ID),
        Mockito.argThat(this::compareArgumentsMinutes));
  }

  @Test
  void testExecuteAllUnitsOk() {
    // WHEN
    final List<DisableDeviationInput> disableDeviationInput = generateDisableDeviationInput(DeviationType.UNITS, AFFECTED_SHIPMENT_TYPES);

    disableDeviation.executeAll(WAREHOUSE_ID, disableDeviationInput);

    // THEN
    verify(planningModelGateway).disableDeviationAll(eq(WAREHOUSE_ID),
        Mockito.argThat(res -> compareArgumentsUnits(AFFECTED_SHIPMENT_TYPES, res)));
  }

  @Test
  void testExecuteAllUnitsShipmentTypeNullOk() {
    // WHEN
    final List<DisableDeviationInput> disableDeviationInput = List.of(
        DisableDeviationInput.builder()
            .workflow(INBOUND_TRANSFER)
            .type(DeviationType.UNITS)
            .build()
    );

    disableDeviation.executeAll(WAREHOUSE_ID, disableDeviationInput);

    // THEN
    verify(planningModelGateway).disableDeviationAll(eq(WAREHOUSE_ID),
        Mockito.argThat(res -> compareArgumentsUnits(null, res)));
  }

  private List<DisableDeviationInput> generateDisableDeviationInput(
      final DeviationType type, final List<ShipmentType> affectedShipmentTypes) {

    return List.of(
        DisableDeviationInput.builder()
            .workflow(INBOUND)
            .type(type)
            .affectedShipmentTypes(affectedShipmentTypes)
            .build(),
        DisableDeviationInput.builder()
            .workflow(INBOUND_TRANSFER)
            .type(type)
            .build()
    );
  }

  private boolean compareArgumentsMinutes(final List<DisableDeviationInput> actual) {
    return INBOUND.equals(actual.get(0).getWorkflow())
        && INBOUND_TRANSFER.equals(actual.get(1).getWorkflow())
        && DeviationType.MINUTES.equals(actual.get(0).getType())
        && DeviationType.MINUTES.equals(actual.get(1).getType())
        && AFFECTED_SHIPMENT_TYPES.equals(actual.get(0).getAffectedShipmentTypes());
  }

  private boolean compareArgumentsUnits(final List<ShipmentType> affectedShipmentTypes, final List<DisableDeviationInput> actual) {
    return !INBOUND.equals(actual.get(0).getWorkflow())
        || !INBOUND_TRANSFER.equals(actual.get(1).getWorkflow())
        || !DeviationType.UNITS.equals(actual.get(0).getType())
        || !DeviationType.UNITS.equals(actual.get(1).getType())
        || affectedShipmentTypes == null || affectedShipmentTypes.equals(actual.get(0).getAffectedShipmentTypes());
  }
}
