package com.mercadolibre.planning.model.me.entities.projection;

import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.monitoring.Monitoring;
import java.util.List;
import lombok.Getter;

@Getter
public class DeferralResultData extends ResultData {

  private final Monitoring monitoring;

  public DeferralResultData(ComplexTable complexTable1, List<Projection> projections, Monitoring monitoring) {
    super(complexTable1, projections);
    this.monitoring = monitoring;
  }
}
