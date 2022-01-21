package com.mercadolibre.planning.model.me.entities.projection.dateselector;

import lombok.Value;

@Value
public class DateSelector {

    private String title;

    private Date[] dates;
}
