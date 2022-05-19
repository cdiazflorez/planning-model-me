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

  public NumberOfUnitsInAnArea(final String area,
                               final List<NumberOfUnitsInASubarea> subareas,
                               final Integer reps,
                               final Double repsPercentage) {
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

    public NumberOfUnitsInASubarea(final String area, final int units) {
      this.name = area;
      this.units = units;
      this.reps = null;
      this.repsPercentage = null;
    }

    public NumberOfUnitsInASubarea(final String area, final int units, final Integer reps, final Double repsPercentage) {
      this.name = area;
      this.units = units;
      this.reps = reps;
      this.repsPercentage = repsPercentage;
    }

  }
}
