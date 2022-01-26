package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import lombok.Value;

@Value
public class NumberOfUnitsInAnArea {
    String area;
    Integer units;
}
