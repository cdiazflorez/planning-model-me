package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import java.util.List;
import lombok.Value;


@Value
public class ResultData {
  @JsonProperty("complex_table_1")
  ComplexTable complexTable1;

  List<Projection> projections;
}
