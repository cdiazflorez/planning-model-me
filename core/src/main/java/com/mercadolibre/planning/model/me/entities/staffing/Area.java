package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Value;

@Value
public class Area {

    private String area;

    private Integer netProductivity;

    private Worker workers;
}
