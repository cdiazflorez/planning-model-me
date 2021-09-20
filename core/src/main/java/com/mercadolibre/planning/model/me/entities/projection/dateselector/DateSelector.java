package com.mercadolibre.planning.model.me.entities.projection.dateselector;

import lombok.Value;

@Value
public class DateSelector {

    public String title;

    public Date[] dates;
}
