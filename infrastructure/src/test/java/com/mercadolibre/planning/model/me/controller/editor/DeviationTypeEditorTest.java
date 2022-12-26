package com.mercadolibre.planning.model.me.controller.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DeviationTypeEditorTest {

  private final DeviationTypeEditor editor = new DeviationTypeEditor();

  @Test
  @DisplayName("setAsText should deviationType")
  public void testGetOk() {
    // WHEN
    editor.setAsText(DeviationType.UNITS.getName());

    // THEN
    assertAll(
        () -> assertEquals(DeviationType.class, editor.getValue().getClass()),
        () -> assertEquals(DeviationType.UNITS, editor.getValue())
    );
  }

  @Test
  @DisplayName("invalid deviationType")
  public void invalidText() {
    // WHEN
    final IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> editor.setAsText("")
    );

    // THEN
    final String expectedMessage = "Value should not be blank";

    assertEquals(expectedMessage, exception.getMessage());
  }
}
