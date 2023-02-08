package com.mercadolibre.planning.model.me.exception;

public class InvalidParamException extends RuntimeException {

  private static final long serialVersionUID = -8460356990632230194L;

  public InvalidParamException(final String message) {
    super(message);
  }
}
