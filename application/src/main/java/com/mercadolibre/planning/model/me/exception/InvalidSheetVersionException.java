package com.mercadolibre.planning.model.me.exception;

public class InvalidSheetVersionException extends RuntimeException {

  private static final long serialVersionUID = -2387459234057398475L;

  public InvalidSheetVersionException(final String message) {
    super(message);
  }
}
