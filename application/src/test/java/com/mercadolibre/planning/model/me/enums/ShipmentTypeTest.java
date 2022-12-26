package com.mercadolibre.planning.model.me.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ShipmentTypeTest {

  @Test
  public void testSchedulesPathOk() {
    assertEquals(ShipmentType.COLLECT.getName(), "collect");
    assertEquals(ShipmentType.TRANSFER_SHIPMENT.getName(), "transfer-shipment");
  }
}
