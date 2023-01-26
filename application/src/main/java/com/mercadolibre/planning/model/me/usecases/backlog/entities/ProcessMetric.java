package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Indicator;
import lombok.Value;

@Value
public class ProcessMetric {
  Indicator total;
  Indicator immediate;
}
