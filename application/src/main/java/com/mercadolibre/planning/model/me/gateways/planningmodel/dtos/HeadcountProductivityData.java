package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class HeadcountProductivityData {
    private ZonedDateTime dayTime;
    private long productivity;
}
