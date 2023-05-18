package com.mercadolibre.planning.model.me.exception;

public class LowerAndUpperLimitsException extends RuntimeException {

  private static final long serialVersionUID = -422;

  public LowerAndUpperLimitsException(final String message) {
    super(message);
  }
}
