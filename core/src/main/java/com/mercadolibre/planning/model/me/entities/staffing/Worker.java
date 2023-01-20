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

  private Integer nonSystemicPlanned;

  private Integer delta;

  private Integer nonSystemicDelta;

  public Worker(Integer idle, Integer busy) {
    this.idle = idle;
    this.busy = busy;
    this.nonSystemic = null;
    this.planned = null;
    this.nonSystemicPlanned = null;
    this.delta = null;
    this.nonSystemicDelta = null;
  }
}
