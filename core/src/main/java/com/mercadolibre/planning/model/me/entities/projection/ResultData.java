package com.mercadolibre.planning.model.me.entities.projection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.monitoring.Monitoring;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;


@Getter
@RequiredArgsConstructor
public class ResultData {

  @JsonProperty("complex_table_1")
  private final ComplexTable complexTable1;

  private final List<Projection> projections;
}
