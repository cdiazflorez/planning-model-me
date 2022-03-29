package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NonSystemicWorkers {
  private Integer total;

  private Integer subProcesses;

  private Integer cross;
}
