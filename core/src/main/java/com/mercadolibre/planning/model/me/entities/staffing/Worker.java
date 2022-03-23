package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

@Value
public class Worker {

  private Integer idle;

  private Integer busy;

  private Integer planned;

  private Integer delta;
}
