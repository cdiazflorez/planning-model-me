package com.mercadolibre.planning.model.me.usecases.projection.entities;

import java.util.List;
import lombok.Value;

@Value
public class HeadcountAtArea {

  String area;

  Integer reps;

  Double respPercentage;

  List<HeadcountBySubArea> subAreas;
}
