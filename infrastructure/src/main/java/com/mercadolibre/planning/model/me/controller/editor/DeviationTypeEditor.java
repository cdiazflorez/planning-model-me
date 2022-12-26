package com.mercadolibre.planning.model.me.controller.editor;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import java.beans.PropertyEditorSupport;
import java.util.Locale;

public class DeviationTypeEditor extends PropertyEditorSupport {
  @Override
  public void setAsText(final String text) {
    if (isBlank(text)) {
      throw new IllegalArgumentException("Value should not be blank");
    }

    setValue(DeviationType.valueOf(text.toUpperCase(Locale.ROOT)));
  }
}
