package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
public class HeadcountProductivityData {
    private ZonedDateTime dayTime;
    private long productivity;
}
