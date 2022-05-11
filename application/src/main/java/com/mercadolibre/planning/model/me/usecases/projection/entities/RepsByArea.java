package com.mercadolibre.planning.model.me.usecases.projection.entities;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class RepsByArea {

  String area;
  Double reps;
}
