package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Worker {

  private Integer idle;

  private Integer busy;

  private Integer nonSystemic;

  private Integer planned;

  private Integer delta;

  public Worker(Integer idle, Integer busy) {
    this.idle = idle;
    this.busy = busy;
    this.nonSystemic = null;
    this.planned = null;
    this.delta = null;
  }
}
