package com.mercadolibre.planning.model.me.usecases.projection.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class HeadcountBySubArea {

  String subArea;

  Integer reps;

  Double respPercentage;
}
