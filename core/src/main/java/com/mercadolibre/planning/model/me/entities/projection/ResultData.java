package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class ResultData {

  @JsonProperty("complex_table_1")
  private final ComplexTable complexTable1;

  private final List<Projection> projections;
}
