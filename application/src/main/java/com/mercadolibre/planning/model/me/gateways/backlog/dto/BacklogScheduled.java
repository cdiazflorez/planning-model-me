package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import lombok.Value;

@Value
public class BacklogScheduled {
  Indicator expected;
  Indicator received;
  Indicator pending;
  Indicator deviation;
}
