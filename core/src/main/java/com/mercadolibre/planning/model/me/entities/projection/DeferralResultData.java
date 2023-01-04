package com.mercadolibre.planning.model.me.entities.projection;

import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.monitoring.Monitoring;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
public class DeferralResultData extends ResultData {

  Monitoring monitoring;

  public DeferralResultData(ComplexTable complexTable1, List<Projection> projections, Monitoring monitoring) {
    super(complexTable1, projections);
    this.monitoring = monitoring;
  }
}
