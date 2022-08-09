package com.mercadolibre.planning.model.me.services.backlog;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BacklogGrouper {
  AREA,
  CARRIER,
  DATE_IN,
  DATE_OUT,
  PROCESS,
  STEP,
  STATUS,
  WORKFLOW;

  @JsonValue
  public String getName() {
    return this.toString().toLowerCase();
  }
}
