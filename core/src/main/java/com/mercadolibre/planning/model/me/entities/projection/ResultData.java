package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.monitoring.Monitoring;
import java.util.List;
import lombok.Value;


@Value
public class ResultData {
  @JsonProperty("complex_table_1")
  ComplexTable complexTable1;

  List<Projection> projections;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = Monitoring.class)
  Monitoring monitoring;

  public ResultData(ComplexTable complexTable1, List<Projection> projections) {
    this.complexTable1 = complexTable1;
    this.projections = projections;
    this.monitoring = new Monitoring();
  }

  public ResultData(ComplexTable complexTable1, List<Projection> projections, Monitoring monitoring) {
    this.complexTable1 = complexTable1;
    this.projections = projections;
    this.monitoring = monitoring;
  }

}
