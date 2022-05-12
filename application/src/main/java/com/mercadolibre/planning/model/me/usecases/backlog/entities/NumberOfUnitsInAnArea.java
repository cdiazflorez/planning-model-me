package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import static java.util.Collections.emptyList;

import java.util.List;
import lombok.Value;

@Value
public class NumberOfUnitsInAnArea {
  String area;

  Integer units;

  List<NumberOfUnitsInASubarea> subareas;

  public NumberOfUnitsInAnArea(final String area, final Integer units) {
    this.area = area;
    this.units = units;
    this.subareas = emptyList();
  }

  public NumberOfUnitsInAnArea(final String area, final List<NumberOfUnitsInASubarea> subareas) {
    this.area = area;
    this.subareas = subareas;
    this.units = subareas.stream()
        .mapToInt(NumberOfUnitsInASubarea::getUnits)
        .sum();
  }

  @Value
  public static class NumberOfUnitsInASubarea {
    String name;

    Integer units;
  }
}
