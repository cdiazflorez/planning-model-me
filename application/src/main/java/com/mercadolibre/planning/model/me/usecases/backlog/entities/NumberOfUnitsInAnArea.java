package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import static java.util.Collections.emptyList;

import java.util.List;
import lombok.Value;

@Value
public class NumberOfUnitsInAnArea {
  String area;

  Integer units;

  Integer reps;

  Double repsPercentage;

  List<NumberOfUnitsInASubarea> subareas;

  public NumberOfUnitsInAnArea(final String area, final Integer units) {
    this.area = area;
    this.units = units;
    this.subareas = emptyList();
    this.reps = null;
    this.repsPercentage = null;
  }

  public NumberOfUnitsInAnArea(final String area, final List<NumberOfUnitsInASubarea> subareas) {
    this.area = area;
    this.subareas = subareas;
    this.units = subareas.stream()
        .mapToInt(NumberOfUnitsInASubarea::getUnits)
        .sum();
    this.reps = null;
    this.repsPercentage = null;
  }

  public NumberOfUnitsInAnArea(final String area, final List<NumberOfUnitsInASubarea> subareas, Integer reps, Double repsPercentage) {
    this.area = area;
    this.subareas = subareas;
    this.units = subareas.stream()
        .mapToInt(NumberOfUnitsInASubarea::getUnits)
        .sum();
    this.reps = reps;
    this.repsPercentage = repsPercentage;
  }

  @Value
  public static class NumberOfUnitsInASubarea {
    String name;

    Integer units;

    Integer reps;

    Double repsPercentage;

    public NumberOfUnitsInASubarea(String area, int units) {
      this.name = area;
      this.units = units;
      this.reps = null;
      this.repsPercentage = null;
    }

    public NumberOfUnitsInASubarea(String area, int units, Integer reps, Double repsPercentage) {
      this.name = area;
      this.units = units;
      this.reps = reps;
      this.repsPercentage = repsPercentage;
    }

  }
}
