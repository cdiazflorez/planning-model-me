package com.mercadolibre.planning.model.me.usecases.deviation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
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
  void testSaveOk() {
    // WHEN
    saveDeviation.execute(SAVE_DEVIATION_INPUTS);

    // THEN
    verify(deviationGateway).save(SAVE_DEVIATION_INPUTS);
    verifyNoInteractions(planningModelGateway);
  }
}
