package com.mercadolibre.planning.model.me.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CodeError {
  SBO001("Negative values not allowed for workers and active_workers");

  private final String message;

  public String getMessage() {
    return this.message;
  }

  public String getName() {
    return this.toString();
  }
}
