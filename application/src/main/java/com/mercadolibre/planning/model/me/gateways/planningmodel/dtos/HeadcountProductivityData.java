package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetTime;

@Getter
@Builder
public class HeadcountProductivityData {
    private OffsetTime dayTime;
    private long productivity;
}
