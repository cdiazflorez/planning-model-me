package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class HeadcountProductivityData {
    private ZonedDateTime dayTime;
    private long productivity;
}
