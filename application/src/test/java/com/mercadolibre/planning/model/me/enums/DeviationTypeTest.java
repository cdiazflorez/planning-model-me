package com.mercadolibre.planning.model.me.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class DeviationTypeTest {

  @ParameterizedTest
  @EnumSource(value = DeviationType.class)
  public void testSchedulesPathOk(final DeviationType deviationType) {
    assertFalse(deviationType.getName().isEmpty());
  }
}
