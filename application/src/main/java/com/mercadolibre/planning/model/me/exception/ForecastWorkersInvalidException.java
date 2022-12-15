package com.mercadolibre.planning.model.me.exception;

import lombok.Getter;

@Getter
public class ForecastWorkersInvalidException extends RuntimeException {

  private static final long serialVersionUID = 4955135398329582323L;

  private final String code;

  public ForecastWorkersInvalidException(final String message, final String code) {
    super(message);
    this.code = code;
  }
}
