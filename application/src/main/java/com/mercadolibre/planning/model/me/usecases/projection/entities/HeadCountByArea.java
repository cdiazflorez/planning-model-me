package com.mercadolibre.planning.model.me.usecases.projection.entities;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class HeadCountByArea {

  String area;
  Integer reps;
  Double respPercentage;
  List<HeadcountBySubArea> subAreas;
}
