package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Getter;

@Getter
public class PlannedWorker extends Worker {
  private final Integer planned;

  private final Integer nonSystemicPlanned;

  private final Integer delta;

  private final Integer nonSystemicDelta;

  public PlannedWorker(
      Integer idle,
      Integer busy,
      Integer nonSystemic,
      Integer planned,
      Integer nonSystemicPlanned,
      Integer delta,
      Integer nonSystemicDelta) {
    super(idle, busy, nonSystemic);
    this.planned = planned;
    this.nonSystemicPlanned = nonSystemicPlanned;
    this.delta = delta;
    this.nonSystemicDelta = nonSystemicDelta;
  }
}
