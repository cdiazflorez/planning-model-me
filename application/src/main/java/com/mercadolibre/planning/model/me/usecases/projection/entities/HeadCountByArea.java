package com.mercadolibre.planning.model.me.usecases.projection.entities;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class HeadCountByArea {

  String area;
  Integer reps;
  Double respPercentage;
  List<HeadcountBySubArea> subAreas;
}
